package com.thaison.barcodescannerexample1;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnStart;
    private Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        initViews();
    }

    private void initViews() {
        btnStart = (Button)findViewById(R.id.btn_start);
        btnStart.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        intent = new Intent(this, ScannerActivity.class);
        startActivity(intent);
    }
}
