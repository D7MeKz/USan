package com.example.usan_comb1.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.usan_comb1.R;
import com.example.usan_comb1.activity.FavoriteActivity;
import com.example.usan_comb1.map.ListOnline;
import com.example.usan_comb1.activity.SalelistActivity;
import com.example.usan_comb1.activity.UserUpdateActivity;
import com.example.usan_comb1.map.MapActivity;

public class UserFragment extends Fragment {

    private Button btnviewprf, btnsalelist, btnwishlist, btnimageup;
    private Button btngps;
    private ImageView imageView;
    private TextView tvname;

    private String username;
    private String accessToken;


    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);


        btnviewprf = view.findViewById(R.id.btnviewprf);
        btnwishlist = view.findViewById(R.id.btnwishlist);
        btnsalelist = view.findViewById(R.id.btnsalelist);
        imageView = view.findViewById(R.id.imageView);
        tvname = view.findViewById(R.id.name);

        btngps = view.findViewById(R.id.btngps);

        SharedPreferences prefs = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        accessToken = prefs.getString("access_token", "");
        username = prefs.getString("username", "");

        tvname.setText(username);


        btnviewprf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserUpdateActivity.class);
                startActivity(intent);
            }
        });

        btnwishlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent 정보를 넘겨줌
                Intent intent = getActivity().getIntent();
                String username = intent.getStringExtra("username");

                Intent wishlistIntent = new Intent(getActivity(), FavoriteActivity.class);
                wishlistIntent.putExtra("username", username);
                System.out.println("Fragment Wishlist : "+ username);
                startActivity(wishlistIntent);
            }
        });

        btnsalelist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent 정보를 넘겨줌
                Intent intent = getActivity().getIntent();
                String username = intent.getStringExtra("username");

                Intent salelistIntent = new Intent(getActivity(), SalelistActivity.class);
                salelistIntent.putExtra("username", username);
                System.out.println("Fragment Salelist : "+ username);
                startActivity(salelistIntent);
            }
        });

        btngps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MapActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }



}