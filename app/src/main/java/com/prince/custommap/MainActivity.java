package com.prince.custommap;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView mTextView;
    private Button mButton;
    private final int RC_CODE=5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView=(TextView)findViewById(R.id.textView);
        mButton=(Button)findViewById(R.id.button);

        //Calling the maps activity
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,MapsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(i,RC_CODE);
            }
        });
    }


    //Displaying the data sent by maps activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case RC_CODE:
                if(resultCode==RESULT_OK){
                    mTextView.setText(data.getStringExtra("address1")+"\n"+data.getStringExtra("address2"));
                }
        }
    }
}
