package com.example.imagecompressdemo;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.gouplee.luban.Luban;
import com.gouplee.luban.OnCompressListener;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File file = new File(Environment.getExternalStorageDirectory(), "ImageCompressDemo/compress/abc.jpg");
        File targetDir = new File(Environment.getExternalStorageDirectory(), "ImageCompressDemo/compress");

//        Luban.with(this)
//                .load(file)
//                .setTargetDir(targetDir.getAbsolutePath())
//                .setRenameListener(new OnRenameListener() {
//                    @Override
//                    public String rename(String filePath) {
//                        return "cba.jpg";
//                    }
//                })
//                .setCompressListener(new OnCompressListener() {
//                    @Override
//                    public void onStart() {
//                        Toast.makeText(MainActivity.this, "压缩开始", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onSuccess(File file) {
//                        Log.e("aaaaaa", file.getAbsolutePath());
//                        Toast.makeText(MainActivity.this, "压缩完成", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Toast.makeText(MainActivity.this, "压缩失败", Toast.LENGTH_SHORT).show();
//                    }
//                }).launch();


        Luban.with(this)
                .load(file)
                .setTargetDir(targetDir.getAbsolutePath())
                .ignoreBy(50)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        Toast.makeText(MainActivity.this, "开始压缩", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(File file) {
                        Toast.makeText(MainActivity.this, "路径：" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, "压缩失败", Toast.LENGTH_SHORT).show();
                    }
                }).launch();
    }
}