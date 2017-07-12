package com.example.dong.emotionapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISION_CODE = 101;
    ImageView imageView;
    Button btn_Take, btn_Process;
    EmotionServiceClient restClient= new EmotionServiceRestClient("8f48bda17fc24696a027fa337c0be1b4");
    int TAKE_PIC_CODE=100;
    Bitmap mBitmap;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==REQUEST_PERMISION_CODE){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initView();

        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
            },REQUEST_PERMISION_CODE);

        }
    }

    private void initView() {
        btn_Take= (Button) findViewById(R.id.btn_take);
        btn_Process= (Button) findViewById(R.id.btn_process);
        imageView= (ImageView) findViewById(R.id.imageview);

        btn_Take.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                takePictureFromGallery();
            }
        });

        btn_Process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processImage();
            }
        });
    }

    private void processImage() {
        //convert image to stream
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        ByteArrayInputStream inputStream=new ByteArrayInputStream(outputStream.toByteArray());

        //create asyntask to process data
        AsyncTask<InputStream,String,List<RecognizeResult>> processAsync=new AsyncTask<InputStream, String, List<RecognizeResult>>() {
            ProgressDialog mDialog=new ProgressDialog(MainActivity.this);

            @Override
            protected void onPreExecute() {
                mDialog.show();
            }

            @Override
            protected List<RecognizeResult> doInBackground(InputStream... params) {
                publishProgress("Please wait...");
                List<RecognizeResult> result= null;
                try {
                    result = restClient.recognizeImage(params[0]);
                } catch (EmotionServiceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                mDialog.setMessage(values[0]);
            }

            @Override
            protected void onPostExecute(List<RecognizeResult> recognizeResults) {
                mDialog.dismiss();
                for(RecognizeResult res:recognizeResults){
                    String status=getEmotion(res);
                    imageView.setImageBitmap(ImageHelper.drawRectOnBitmap(mBitmap,res.faceRectangle,status));
                }
            }
        };

        processAsync.execute(inputStream);
    }

    private String getEmotion(RecognizeResult res) {
        List<Double> list=new ArrayList<>();
        Scores scores=res.scores;
        list.add(scores.anger);
        list.add(scores.happiness);
        list.add(scores.contempt);
        list.add(scores.disgust);
        list.add(scores.fear);
        list.add(scores.neutral);
        list.add(scores.surprise);
        list.add(scores.sadness);

        //sort list
        Collections.sort(list);

        //get max value from list
        double maxNum=list.get(list.size()-1);
        if(maxNum==scores.anger)
            return "Phẫn Nộ";
        else  if(maxNum==scores.happiness)
            return "Vui vẻ";
        else  if(maxNum==scores.contempt)
            return "Khinh Thường ";
        else  if(maxNum==scores.disgust)
            return "Chán ";
        else  if(maxNum==scores.fear)
            return "Sợ Hãi";
        else  if(maxNum==scores.neutral)
            return "Trung Lập";
        else  if(maxNum==scores.surprise)
            return "Ngạc Nhiên";
        else  if(maxNum==scores.sadness)
            return "Buồn";
        else
            return "Can't detect";

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==TAKE_PIC_CODE){
            Uri select=data.getData();
            InputStream in=null;
            try{
                in=getContentResolver().openInputStream(select);
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }
            mBitmap= BitmapFactory.decodeStream(in);
            imageView.setImageBitmap(mBitmap);
        }
    }

    private void takePictureFromGallery() {
        Intent intent=new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,TAKE_PIC_CODE);
    }
}
