package com.example.usan_comb1.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.usan_comb1.ProductService;
import com.example.usan_comb1.R;
import com.example.usan_comb1.RetrofitClient;

import com.example.usan_comb1.activity.product.DetailActivity;
import com.example.usan_comb1.activity.product.UploadActivity;
import com.example.usan_comb1.adapter.HomeAdapter;
import com.example.usan_comb1.response.PostList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    FloatingActionButton fab;
    private String accessToken;
    //private int currentPage = 1; // 페이지 번호 변수 선언

    private boolean isDataLoading = false; // 데이터 로딩 여부를 관리하는 변수

    NestedScrollView nestedScrollView;
    ProgressBar progressBar;

    ArrayList<PostList> dataArrayList = new ArrayList<>();

    private ProductService mProductService;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SharedPreferences prefs = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        accessToken = prefs.getString("access_token", "");

        mProductService = RetrofitClient.getRetrofitInstance().create(ProductService.class);

        nestedScrollView = view.findViewById(R.id.scroll_view);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);

        adapter = new HomeAdapter(getActivity(), dataArrayList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        // CustomAdapter의 아이템 클릭 리스너를 설정합니다.
        adapter.setOnItemClickListener(new HomeAdapter.OnItemClickListener() {
            // 아이템을 클릭했을 때 다른 액티비티로 넘어가는 코드를 추가합니다.

            public void onItemClick(View view, int position, PostList data) {
                Intent intent = getActivity().getIntent();
                String username = intent.getStringExtra("username");

                Intent ProductIntent = new Intent(getActivity(), DetailActivity.class);
                ProductIntent.putExtra("product_id", data.getProduct_id());// 넘어갈 데이터를 인텐트에 추가합니다.
                ProductIntent.putExtra("username", username);
                startActivity(ProductIntent);
            }
        });


        getData();

        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener()
        {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY)
            {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())
                {
                    progressBar.setVisibility(View.VISIBLE);
                    getData();
                }
            }
        });


        // 업로드 페이지로 이동하는 버튼

        fab = view.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), UploadActivity.class);
                startActivity(intent);
            }
        });


        return view;
    }

    private void getData()
    {
        if (isDataLoading) {
            return; // 이미 데이터 로딩 중인 경우 중복 호출 방지
        }

        dataArrayList.clear(); // 기존 데이터 초기화

        isDataLoading = true; // 데이터 로딩 시작

        ProductService productService = RetrofitClient.getProductService();
        //Call<String> call = productService.string_call(accessToken, currentPage); // 페이지 번호 설정
        Call<String> call = productService.string_call(accessToken);
        call.enqueue(new Callback<String>()
        {
            @Override
            public void onResponse(Call<String> call, Response<String> response)
            {
                isDataLoading = false; // 데이터 로딩 종료
                if (response.isSuccessful() && response.body() != null)
                {
                    progressBar.setVisibility(View.GONE);
                    try
                    {
                        JSONArray jsonArray = new JSONArray(response.body());
                        parseResult(jsonArray);
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t)
            {
                isDataLoading = false; // 데이터 로딩 종료
                Log.e("에러 : ", t.getMessage());
            }
        });
    }

    private void parseResult(JSONArray jsonArray)
    {
        for (int i = 0; i < jsonArray.length(); i++)
        {
            try
            {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                PostList data = new PostList();
                data.setImg(jsonObject.getString("img"));
                data.setTitle(jsonObject.getString("title"));
                data.setAuthor(jsonObject.getString("author"));
                data.setPrice(jsonObject.getString("price"));
                data.setProduct_id(jsonObject.getInt("product_id"));
                dataArrayList.add(data);

                //currentPage++; // 페이지 번호 증가
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            adapter.notifyDataSetChanged(); // Adapter에 데이터 변경을 알려줌
        }
    }
}