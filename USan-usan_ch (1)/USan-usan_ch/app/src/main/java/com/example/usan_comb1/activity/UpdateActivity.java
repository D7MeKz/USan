package com.example.usan_comb1.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.usan_comb1.ProductService;
import com.example.usan_comb1.R;
import com.example.usan_comb1.RetrofitClient;
import com.example.usan_comb1.request.UpdateRequest;
import com.example.usan_comb1.response.ProductImageResponse;
import com.example.usan_comb1.response.UpdateResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 상품 수정 Activity
public class UpdateActivity extends AppCompatActivity {

    private EditText eTitle;
    private EditText eContent;
    private EditText eAddress;
    private EditText ePrice;
    private ImageView productImg;
    private Integer productId;
    private String username, accessToken;
    private static final int REQUEST_SELECT_IMAGE = 2;
    private static final int REQUEST_CROP_IMAGE = 3;
    private static final int REQUEST_READ_MEDIA_IMAGES = 1;
    private Uri imageUri; // Added variable to store selected image URI
    private String path;
    private String filename;

    private ProgressBar mProgressView;
    private ProductService mProductService;
    private UpdateRequest.Address addressObj;

    private UpdateResponse previousProduct; // 이전에 올린 게시글의 내용을 담을 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        eTitle = findViewById(R.id.updateTitle);
        eContent = findViewById(R.id.updateContent);
        eAddress = findViewById(R.id.updateAddress);
        ePrice = findViewById(R.id.updatePrice);
        productImg = findViewById(R.id.productImage);

        mProgressView = (ProgressBar) findViewById(R.id.product_progress);

        mProductService = RetrofitClient.getRetrofitInstance().create(ProductService.class);

        // Authorization
        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        accessToken = prefs.getString("access_token", "");

        Intent intent = getIntent();
        username = intent.getStringExtra("username");

        productId = getIntent().getIntExtra("productId", -1);



        if (intent != null) {
            if (productId != -1) {
                getProduct(productId, accessToken);
            }
        }

        downloadImage();

        productImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 파일 엑세스 권한 확인
                verifyStoragePermissions(UpdateActivity.this);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                updateData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateData() {
        eTitle.setError(null);
        eContent.setError(null);
        eAddress.setError(null);
        ePrice.setError(null);

        boolean cancel = false;
        View focusView = null;

        String title = eTitle.getText().toString();
        String content = eContent.getText().toString();
        String address = eAddress.getText().toString();
        String price = ePrice.getText().toString();

        // 제목의 유효성 검사
        if (title.isEmpty()) {
            eTitle.setError("제목을 입력해주세요.");
            focusView = eTitle;
            //title = "None";
            cancel = true;
        }

        // 내용의 유효성 검사
        if (content.isEmpty()) {
            eContent.setError("내용을 입력해주세요.");
            focusView = eContent;
            cancel = true;
            //content = "None";
        }

        // 주소의 유효성 검사
        if (address.isEmpty()) {
            eAddress.setError("주소를 입력해주세요.");
            focusView = eAddress;
            cancel = true;
            //address = "None";
        }

        // 가격의 유효성 검사
        if (price.isEmpty()) {
            ePrice.setError("가격을 입력해주세요.");
            focusView = ePrice;
            cancel = true;
            //price = "None";
        }


        // 이전 게시글의 내용을 업데이트하기 위해 UpdateRequest 객체 생성
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setProduct_id(productId);
        updateRequest.setTitle(title);
        updateRequest.setContent(content);
        updateRequest.setPrice(price);

        UpdateRequest.Address updateAddress = new UpdateRequest.Address("None", 0.0, 0.0);
        updateRequest.setAddress(updateAddress);


        Call<UpdateRequest> call = mProductService.updateProduct(accessToken, productId, updateRequest);
        call.enqueue(new Callback<UpdateRequest>() {
            @Override
            public void onResponse(Call<UpdateRequest> call, Response<UpdateRequest> response) {
                if (response.isSuccessful()) {

                    // 성공적으로 업데이트된 경우
                    showUpdateSuccessDialog();
                } else {
                    // 업데이트 실패한 경우
                    Toast.makeText(UpdateActivity.this, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateRequest> call, Throwable t) {
                // 통신 실패한 경우
                Toast.makeText(UpdateActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        if (cancel) {
            focusView.requestFocus();
        } else {
            GeocodeAsyncTask task = new GeocodeAsyncTask();
            task.execute(address);
            showProgress(true);
        }
    }


    private void getProduct(Integer productId, String accessToken) {
        Call<UpdateResponse> call = mProductService.getupdateProduct(accessToken, productId);
        call.enqueue(new Callback<UpdateResponse>() {
            @Override
            public void onResponse(Call<UpdateResponse> call, Response<UpdateResponse> response) {
                if (response.isSuccessful()) {
                    UpdateResponse product = response.body();
                    if (product != null) {
                        // 이전에 올린 게시글의 내용을 변수에 저장
                        previousProduct = new UpdateResponse();
                        previousProduct.setProduct_id(product.getProduct_id());
                        previousProduct.setTitle(product.getTitle());
                        previousProduct.setContent(product.getContent());
                        previousProduct.setAddress(product.getAddress());
                        previousProduct.setPrice(product.getPrice());

                        // 이전 게시글의 내용을 화면에 표시
                        eTitle.setText(product.getTitle());
                        eContent.setText(product.getContent());
                        eAddress.setText(product.getAddress().getName());
                        ePrice.setText(product.getPrice());
                    }
                } else {
                    Toast.makeText(UpdateActivity.this, "게시글을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateResponse> call, Throwable t) {
                Toast.makeText(UpdateActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("상품 정보가 성공적으로 수정되었습니다.");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class GeocodeAsyncTask extends AsyncTask<String, Void, Address> {

        @Override
        protected Address doInBackground(String... strings) {
            String address = strings[0];
            Geocoder geocoder = new Geocoder(UpdateActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if (addresses.size() > 0) {
                    return addresses.get(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Address locationAddress) {
            if (locationAddress != null) {
                double latitude = locationAddress.getLatitude();
                double longitude = locationAddress.getLongitude();
                //showToast("위도 : " + latitude + "\n경도 : " + longitude);

                addressObj = new UpdateRequest.Address(locationAddress.getAddressLine(0), latitude, longitude);
                updateData();
            } else {
                showToast("위치를 찾을 수 없습니다.");
            }
        }
    }

    private void showProgress(boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // 토스트 메시지를 출력하는 메서드
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have permission
        int permission = ActivityCompat.checkSelfPermission(
                activity, android.Manifest.permission.READ_MEDIA_IMAGES);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_READ_MEDIA_IMAGES
            );
        } else {
            // Permission granted, open the gallery
            openGallery(activity);
        }
    }

    private static void openGallery(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        activity.startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri imageUri = clipData.getItemAt(i).getUri();
                    String imagePath = getRealPathFromUri(imageUri);
                    if (imagePath != null) {
                        uploadproductImage(accessToken, productId, imagePath);
                    }
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                String imagePath = getRealPathFromUri(imageUri);
                if (imagePath != null) {
                    uploadproductImage(accessToken, productId, imagePath);
                }
            }
        }
    }


    private String getRealPathFromUri(Uri uri) {
        if (uri == null) {
            // Handle null Uri
            return null;
        }

        String[] projection = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        if (cursor == null) {
            // Handle null cursor
            return null;
        }
        int columnIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(columnIdx);
        cursor.close();
        return result;
    }


    private void uploadproductImage(String accessToken, int productId, String imagePath) {
        String actualPath = Uri.parse(imagePath).getPath();
        File file = new File(actualPath);

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("imgs", file.getName(), requestFile);
        Call<List<ProductImageResponse>> call = mProductService.uploadproductImage(accessToken, productId, body);
        call.enqueue(new Callback<List<ProductImageResponse>>() {
            @Override
            public void onResponse(Call<List<ProductImageResponse>> call, Response<List<ProductImageResponse>> response) {
                if (response.isSuccessful()) {
                    List<ProductImageResponse> imageResponses = response.body();
                    if (imageResponses != null && !imageResponses.isEmpty()) {
                        // 이미지 업로드 성공 처리
                        Toast.makeText(UpdateActivity.this, "사진을 업로드했습니다.", Toast.LENGTH_SHORT).show();
                        Log.i("Upload success", "Successfully uploaded image");
                        filename = ProductImageResponse.getFileName(imageResponses.get(0));
                        System.out.println(imageResponses);
                        if (imageResponses != null && !imageResponses.isEmpty()) {
                            System.out.println(filename);

                            downloadImage();
                        }
                    } else {
                        // 서버 응답에 이미지 정보가 없는 경우 처리
                        Toast.makeText(UpdateActivity.this, "서버 응답에 이미지 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 이미지 업로드 실패 처리
                    Toast.makeText(UpdateActivity.this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("Upload error", "Upload failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ProductImageResponse>> call, Throwable t) {
                // 네트워크 오류 처리
                Toast.makeText(UpdateActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                Log.e("Upload error", t.getMessage());
            }
        });
    }


    // 이미지 다운로드
    private void downloadImage() {
        Call<ResponseBody> call = mProductService.downloadImage(accessToken, productId, filename);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        // 이미지 데이터를 읽어옵니다.
                        InputStream inputStream = responseBody.byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        // 이미지를 이미지 뷰에 설정합니다.
                        productImg.setImageBitmap(bitmap);
                    } else {
                        // 이미지 데이터가 없는 경우 기본 이미지를 설정합니다.
                        productImg.setImageResource(R.drawable.img_error);
                        Toast.makeText(UpdateActivity.this, "이미지가 없습니다.", Toast.LENGTH_SHORT).show();
                        Log.e("Download error", "Download failed: " + response.message());
                    }
                } else {
                    // 서버 응답이 실패인 경우 기본 이미지를 설정합니다.
                    productImg.setImageResource(R.drawable.img_error);
                    Toast.makeText(UpdateActivity.this, "서버 응답 실패", Toast.LENGTH_SHORT).show();
                    Log.e("Download error", "Download failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 이미지 다운로드 중 오류가 발생한 경우 기본 이미지를 설정합니다.
                productImg.setImageResource(R.drawable.img_error);
                Toast.makeText(UpdateActivity.this, "다운로드 오류", Toast.LENGTH_SHORT).show();
                Log.e("Download error", "Download failed: " + t.getMessage());
            }
        });
    }


}