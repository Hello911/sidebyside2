package storage.david.com.sidebyside2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void redirect(View view){
        Intent intent=new Intent();
        switch(view.getId()){
            case R.id.setting:
                intent=new Intent(this, Setting.class);
                break;
            case R.id.photo:
                intent=new Intent(this,Photo.class);
                break;
            case R.id.compare:
                intent=new Intent (this,Compare.class);
                break;
            case R.id.folder:
                intent=new Intent (this,Camera.class);
        }
        startActivity(intent);
    }
}
