package storage.david.com.sidebyside2;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import in.goodiebag.carouselpicker.CarouselPicker;

/**
 * Created by Gongwei (David) Chen on 6/8/2018.
 */

public class Compare extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener{
    Uri uri1;
    Uri uri2;
    ImageView photo1;
    ImageView photo2;
    ImageView selectPhoto1;
    ImageView selectPhoto2;
    ImageView saveCollage;
    ImageView shareCollage;
    ImageView calculateDifference;
    ImageView rotatePhoto1;
    ImageView rotatePhoto2;
    RelativeLayout viewGroup;
    RelativeLayout viewGroupLandscape;
    File imageFile;
    File file;//file path of the collage last taken
    Bitmap viewBitmap;
    private final int PICK_PHOTO1 = 1;
    private final int PICK_PHOTO2 = 2;

    CarouselPicker carouselPicker;
    CarouselPicker carouselPickerLandscape;
    CarouselPicker data1;
    CarouselPicker data2;

    //DRAGGING & ZOOMING
    private static final int NONE=0;
    private static final int DRAG=1;
    private static final int ZOOM=2;

    //set of touch parameters for photo1
    private int mode1=NONE;
    private Matrix matrix1=new Matrix();
    private Matrix savedMatrix1=new Matrix();
    private PointF start1=new PointF();//PointF holds 2 coordinates
    private PointF mid1=new PointF();
    private Bitmap bmap1;
    private float oldDist1=1f;

    //set of touch parameters for photo2
    private int mode2=NONE;
    private Matrix matrix2=new Matrix();
    private Matrix savedMatrix2=new Matrix();
    private PointF start2=new PointF();//PointF holds 2 coordinates
    private PointF mid2=new PointF();
    private Bitmap bmap2;
    private float oldDist2=1f;

    String[] dataArray1;
    String[] dataArray2;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compare);

        viewGroup=(RelativeLayout)findViewById(R.id.viewGroup);//viewGroup of the compare screen
        viewGroupLandscape=(RelativeLayout)findViewById(R.id.mainView);
        photo1 = (ImageView) findViewById(R.id.photo1);
        photo2 = (ImageView) findViewById(R.id.photo2);
        photo1.setOnTouchListener(this);
        photo2.setOnTouchListener(this);


        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        saveCollage=(ImageView) findViewById(R.id.save_collage);
        saveCollage.setOnClickListener(this);

        shareCollage=(ImageView)findViewById(R.id.share_collage);
        shareCollage.setOnClickListener(this);

        calculateDifference=(ImageView) findViewById(R.id.calculateDifference);
        calculateDifference.setOnClickListener(this);

        rotatePhoto1=(ImageView) findViewById(R.id.rotatePhoto1);
        rotatePhoto1.setOnClickListener(this);

        rotatePhoto2=(ImageView) findViewById(R.id.rotatePhoto2);
        rotatePhoto2.setOnClickListener(this);

        selectPhoto1=(ImageView)findViewById(R.id.selectPhoto1);
        selectPhoto1.setOnClickListener(this);
        selectPhoto2=(ImageView)findViewById(R.id.selectPhoto2);
        selectPhoto2.setOnClickListener(this);

        //for COMPARISON portrait mode
        carouselPicker=(CarouselPicker)findViewById(R.id.carousel);
        List<CarouselPicker.PickerItem> textItems = new ArrayList<>();
        //20 here represents the textSize in dp, change it to the value you want.
        textItems.add(new CarouselPicker.TextItem("days", 20));
        textItems.add(new CarouselPicker.TextItem("pounds", 20));
        textItems.add(new CarouselPicker.TextItem("inches", 20));
        textItems.add(new CarouselPicker.TextItem("BMIs", 20));
        CarouselPicker.CarouselViewAdapter textAdapter = new CarouselPicker.CarouselViewAdapter(this, textItems, 0);
        textAdapter.setTextColor(Color.MAGENTA);
        carouselPicker.setAdapter(textAdapter);

        //Everything is the same, but font size is 30 instead of 20.
        //for COMPARISON landscape mode
        carouselPickerLandscape=(CarouselPicker)findViewById(R.id.carouselLandscape);
        List<CarouselPicker.PickerItem> textItemsLandscape = new ArrayList<>();
        //20 here represents the textSize in dp, change it to the value you want.
        textItemsLandscape.add(new CarouselPicker.TextItem("days", 30));
        textItemsLandscape.add(new CarouselPicker.TextItem("pounds", 30));
        textItemsLandscape.add(new CarouselPicker.TextItem("inches", 30));
        textItemsLandscape.add(new CarouselPicker.TextItem("BMIs", 30));
        CarouselPicker.CarouselViewAdapter textAdapterLandscape = new CarouselPicker.CarouselViewAdapter(this, textItemsLandscape, 0);
        textAdapterLandscape.setTextColor(Color.MAGENTA);
        carouselPickerLandscape.setAdapter(textAdapterLandscape);

        //data of first photo
        data1=(CarouselPicker)findViewById(R.id.data1);
        List<CarouselPicker.PickerItem> dataItems1=new ArrayList<>();
        dataItems1.add(new CarouselPicker.TextItem("date",12));
        dataItems1.add(new CarouselPicker.TextItem("weight",12));
        dataItems1.add(new CarouselPicker.TextItem("height",12));
        dataItems1.add(new CarouselPicker.TextItem("BMI",12));
        CarouselPicker.CarouselViewAdapter dataAdapter1=new CarouselPicker.CarouselViewAdapter(this,dataItems1,0);
        dataAdapter1.setTextColor(Color.MAGENTA);
        data1.setAdapter(dataAdapter1);

        //data of second photo
        data2=(CarouselPicker)findViewById(R.id.data2);
        List<CarouselPicker.PickerItem> dataItems2=new ArrayList<>();
        dataItems2.add(new CarouselPicker.TextItem("date",12));
        dataItems2.add(new CarouselPicker.TextItem("weight",12));
        dataItems2.add(new CarouselPicker.TextItem("height",12));
        dataItems2.add(new CarouselPicker.TextItem("BMI",12));
        CarouselPicker.CarouselViewAdapter dataAdapter2=new CarouselPicker.CarouselViewAdapter(this,dataItems2,0);
        dataAdapter2.setTextColor(Color.MAGENTA);
        data2.setAdapter(dataAdapter2);
    }

    /**
     * To make the action bar appear, otherwise it's not there
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compare, menu);
        return true;
    }


    /**ImageView BUTTON Onclick
     * triggered when either imageview is clicked. Differentiated by imageview.ID
     * @param v which imageview is click
     */
    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.share_collage:
                ImageView image=new ImageView(this);
                viewBitmap=getBitmapOfView(viewGroupLandscape);
                image.setImageBitmap(viewBitmap);
                AlertDialog.Builder collageDialog=new AlertDialog.Builder(this)
                        .setView(image)
                        .setTitle("Share Collage")
                        .setPositiveButton("share", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(Compare.this,"Loading....", Toast.LENGTH_SHORT).show();
                                Calendar calendar=Calendar.getInstance();
                                SimpleDateFormat sdformat=new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");
                                String DateString=sdformat.format(calendar.getTime());
                                //get Photo uri
                                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                                viewBitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes);
                                //Force photo to scan for new photos stored.
                                String path=MediaStore.Images.Media.insertImage(Compare.this.getContentResolver(),
                                        viewBitmap,
                                        "mySnapshot_"+DateString+".png",
                                        null);
                                //share photo intent
                                Uri uri=Uri.parse(path);
                                //start sharing Intent
                                Intent intent=new Intent(Intent.ACTION_SEND)
                                        .setType("image/*")
                                        .putExtra(Intent.EXTRA_STREAM,uri);
                                try{
                                    startActivity(Intent.createChooser(intent, "Share collage..."));
                                }catch(ActivityNotFoundException e){
                                    e.printStackTrace();
                                }

                            }//onClick()
                        });
                collageDialog.create().show();
                break;
            case R.id.save_collage:
                ImageView image2=new ImageView(this);
                viewBitmap=getBitmapOfView(viewGroupLandscape);
                image2.setImageBitmap(viewBitmap);
                AlertDialog.Builder collageDialog2=new AlertDialog.Builder(this)
                        .setView(image2)
                        .setTitle("Save Collage")
                        .setPositiveButton("save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try{
                                    Calendar calendar=Calendar.getInstance();
                                    SimpleDateFormat sdformat=new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");
                                    String DateString=sdformat.format(calendar.getTime());

                                    file=new File(getPublicDir(),"mySnapshot_"+DateString+".png");
                                    FileOutputStream fos=new FileOutputStream(file);
                                    viewBitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                                    fos.close();
                                    MediaScannerConnection.scanFile(Compare.this,
                                            new String[]{file.getPath()},
                                            null,
                                            null);
                                    Toast.makeText(Compare.this,"Successfully saved: "+file, Toast.LENGTH_SHORT).show();
                                }catch(IOException e){
                                    e.printStackTrace();
                                }
                            }//onClick()
                        });
                collageDialog2.create().show();
                break;
            case R.id.calculateDifference:
                if(uri1!=null&&uri2!=null) {
                    try {
                        List<CarouselPicker.PickerItem> textItemsLandscape = new ArrayList<>();
                        textItemsLandscape.add(new CarouselPicker.TextItem(getDateDifference(uri1, uri2)+"days", 30));
                        textItemsLandscape.add(new CarouselPicker.TextItem(getDifference(0)+"lbs", 30));
                        textItemsLandscape.add(new CarouselPicker.TextItem(getDifference(1)+"ins", 30));
                        textItemsLandscape.add(new CarouselPicker.TextItem(getDifference(2)+"BMI", 30));

                        CarouselPicker.CarouselViewAdapter textAdapterLandscape = new CarouselPicker.CarouselViewAdapter
                                (this, textItemsLandscape, 0);
                        textAdapterLandscape.setTextColor(Color.MAGENTA);
                        carouselPickerLandscape.setAdapter(textAdapterLandscape);
                    }catch (NullPointerException e){
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(Compare.this,"Cannot calculate days elapsed. Make sure you have picked both photos.",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.rotatePhoto1:
                photo1.setRotation(photo1.getRotation() + 90);
                break;
            case R.id.rotatePhoto2:
                photo2.setRotation(photo2.getRotation() + 90);
                break;
            case R.id.selectPhoto1:
                Intent pickPhoto1 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto1, PICK_PHOTO1);
                break;
            case R.id.selectPhoto2:
                Intent pickPhoto2 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto2, PICK_PHOTO2);
        }
    }

    /**ImageView MEDIA REQUEST HANDLER
     * Do different things based on which imageview is clicked, differentiated by requestCode
     * @param requestCode customized constants
     * @param resultCode  system constant RESULT_OK
     * @param data i dunno. Its an intent.
     */
    @Override
    @TargetApi(24)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //display the 1st select photo, and its data
        if (requestCode == PICK_PHOTO1 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            uri1 = data.getData();
            //reading photo1 Exif data
            InputStream in;
            String dataString1="Not found";
            try{
                in=getContentResolver().openInputStream(uri1);
                ExifInterface exifInterface=new ExifInterface(in);
                dataString1=exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
            }catch(IOException e){
                e.printStackTrace();
            }
            if(dataString1!=null) {//this if else structure is to prevent empty String[] for photos w/o data
                dataArray1 = dataString1.split("\\s+");
            }else{
                dataString1="Nothing is here";
                dataArray1 = dataString1.split("\\s+");
            }
            try {
                List<CarouselPicker.PickerItem> textItems = new ArrayList<>();
                textItems.add(new CarouselPicker.TextItem((String)getDate(uri1), 12));
                textItems.add(new CarouselPicker.TextItem(dataArray1[0]+"lbs", 12));
                textItems.add(new CarouselPicker.TextItem(dataArray1[1]+"ins", 12));
                textItems.add(new CarouselPicker.TextItem(dataArray1[2]+"BMIs", 12));
                CarouselPicker.CarouselViewAdapter textAdapter = new CarouselPicker.CarouselViewAdapter(this, textItems, 0);
                textAdapter.setTextColor(Color.MAGENTA);
                data1.setAdapter(textAdapter);
            }catch(NullPointerException e) {
                e.printStackTrace();
            }
            try {
                Bitmap bitmap1= MediaStore.Images.Media.getBitmap(getContentResolver(), uri1);
                photo1.setImageBitmap(bitmap1);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //display the 2nd select photo, and its data
        if (requestCode == PICK_PHOTO2 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            uri2 = data.getData();
            InputStream in;
            String dataString2="Not found";
            try{
                in=getContentResolver().openInputStream(uri2);
                ExifInterface exifInterface=new ExifInterface(in);
                dataString2=exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
            }catch(IOException e){
                e.printStackTrace();
            }
            if(dataString2!=null) {//this if else structure is to prevent empty String[] for photos w/o data
                dataArray2 = dataString2.split("\\s+");
            }else{
                dataString2="Nothing is here";
                dataArray2 = dataString2.split("\\s+");
            }
            try {
                List<CarouselPicker.PickerItem> textItems = new ArrayList<>();
                textItems.add(new CarouselPicker.TextItem((String)getDate(uri2), 12));
                textItems.add(new CarouselPicker.TextItem(dataArray2[0]+"lbs", 12));
                textItems.add(new CarouselPicker.TextItem(dataArray2[1]+"ins", 12));
                textItems.add(new CarouselPicker.TextItem(dataArray2[2]+"BMIs", 12));
                CarouselPicker.CarouselViewAdapter textAdapter = new CarouselPicker.CarouselViewAdapter(this, textItems, 0);
                textAdapter.setTextColor(Color.MAGENTA);
                data2.setAdapter(textAdapter);
            }catch(NullPointerException e) {
                e.printStackTrace();
            }
            try {
                Bitmap bitmap2= MediaStore.Images.Media.getBitmap(getContentResolver(), uri2);
                photo2.setImageBitmap(bitmap2);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }//OnActivityResult() ends

    /**For TOOLBAR
     * Do different tasks based on which button is clicked.
     * @param item buttons in the top toolbar
     * @return whichever button is clicked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                ImageView image=new ImageView(this);
                viewBitmap=getBitmapOfView(viewGroup);
                image.setImageBitmap(viewBitmap);
                AlertDialog.Builder collageDialog=new AlertDialog.Builder(this)
                        .setView(image)
                        .setTitle("Collage Preview")
                        .setPositiveButton("save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try{
                                    Calendar calendar=Calendar.getInstance();
                                    SimpleDateFormat sdformat=new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");
                                    String DateString=sdformat.format(calendar.getTime());

                                    file=new File(getPublicDir(),"mySnapshot_"+DateString+".png");
                                    FileOutputStream fos=new FileOutputStream(file);
                                    viewBitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                                    fos.close();
                                    MediaScannerConnection.scanFile(Compare.this,
                                            new String[]{file.getPath()},
                                            null,
                                            null);
                                    Toast.makeText(Compare.this,"Successfully saved: "+file, Toast.LENGTH_SHORT).show();
                                }catch(IOException e){
                                    e.printStackTrace();
                                }
                            }//onClick()
                        })
                        .setNegativeButton("share", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(Compare.this,"Loading....", Toast.LENGTH_SHORT).show();
                                Calendar calendar=Calendar.getInstance();
                                SimpleDateFormat sdformat=new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");
                                String DateString=sdformat.format(calendar.getTime());
                                //get Photo uri
                                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                                viewBitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes);
                                String path=MediaStore.Images.Media.insertImage(Compare.this.getContentResolver(),
                                        viewBitmap,
                                        "mySnapshot_"+DateString+".png",
                                        null);
                                Uri uri=Uri.parse(path);
                                //start sharing Intent
                                Intent intent=new Intent(Intent.ACTION_SEND)
                                        .setType("image/*")
                                        .putExtra(Intent.EXTRA_STREAM,uri);
                                try{
                                    startActivity(Intent.createChooser(intent, "Share collage..."));
                                }catch(ActivityNotFoundException e){
                                    e.printStackTrace();
                                }

                            }//onClick()
                        });
                collageDialog.create().show();
                return true;
            case R.id.selectPhoto1:
                Intent pickPhoto1 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto1, PICK_PHOTO1);
                return true;
            case R.id.selectPhoto2:
                Intent pickPhoto2 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto2, PICK_PHOTO2);
                return true;
            case R.id.rotatePhoto1:
                photo1.setRotation(photo1.getRotation() + 90);
                return true;
            case R.id.rotatePhoto2:
                photo2.setRotation(photo2.getRotation() + 90);
                return true;
            case R.id.done:
                if(uri1!=null&&uri2!=null) {
                    try {
                        List<CarouselPicker.PickerItem> textItems = new ArrayList<>();
                        textItems.add(new CarouselPicker.TextItem(getDateDifference(uri1, uri2)+"days", 20));
                        textItems.add(new CarouselPicker.TextItem(getDifference(0)+"lbs", 20));
                        textItems.add(new CarouselPicker.TextItem(getDifference(1)+"ins", 20));
                        textItems.add(new CarouselPicker.TextItem(getDifference(2)+"BMI", 20));
                        CarouselPicker.CarouselViewAdapter textAdapter = new CarouselPicker.CarouselViewAdapter(this, textItems, 0);
                        textAdapter.setTextColor(Color.MAGENTA);
                        carouselPicker.setAdapter(textAdapter);
                    }catch (NullPointerException e){
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(this,"Make sure you have picked both photos.",Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * For getting the date taken of photo with that uri
     * @param photoUri photo uri passed from imageview button handler
     * @return string MM/dd/yyyy
     */
    public CharSequence getDate(Uri photoUri){
        Long longDate=null;
        String[] projection=new String[] {MediaStore.Images.Media.DATE_TAKEN};
        Cursor cur=managedQuery(photoUri,projection,null,null,null);
        if(cur.moveToFirst()){//when cursor is empty
            int dateColumn=cur.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
            longDate = cur.getLong(dateColumn);
        }
        Date d=new Date(longDate);
        java.text.DateFormat formatter=new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(d);
    }

    /**
     *
     * @param photoUri photo to get the description of
     * @return whatever you type into info, photo description
     */
    @TargetApi(25)
    public String getDescription(Uri photoUri){
        InputStream in;
        String description="location";
        try{
            in=getContentResolver().openInputStream(photoUri);
            ExifInterface exifInterface=new ExifInterface(in);
            description=exifInterface.getAttribute(ExifInterface.TAG_SUBJECT_LOCATION);
        }catch(IOException e){
            e.printStackTrace();
        }
        return description;
    }

    /**
     *
     * @param uri1
     * @param uri2
     * @return String difference in days (Date of uri2-Date of uri1)
     */
    public String getDateDifference(Uri uri1, Uri uri2){
        Long longDate1=null;//UNIX time in millisec
        Long longDate2=null;
        Long differenceInDays=null;

        String[] projection=new String[] {MediaStore.Images.Media.DATE_TAKEN};
        Cursor cur1=managedQuery(uri1,projection,null,null,null);

        //get UNIX time for first photo
        if(cur1.moveToFirst()){
            int dateColumn=cur1.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
            do{
                longDate1=cur1.getLong(dateColumn);
            }while(cur1.moveToNext());
        }

        //get UNIX time for second photo
        Cursor cur2=managedQuery(uri2,projection,null,null,null);
        if(cur2.moveToFirst()){
            int dateColumn=cur2.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
            do{
                longDate2=cur2.getLong(dateColumn);
            }while(cur2.moveToNext());
        }

        differenceInDays=(longDate2-longDate1)/1000/60/60/24;
        return differenceInDays.toString();
    }

    /**
     * returns the string difference of string2-string1
     * @param index 0 weight, 1 height, 2 BMI
     * @return
     */
    public String getDifference(int index){
        int[] intDifference={1,2,3};
        String[] stringDifference={"1","2","3"};
        for (int j=0; j<dataArray1.length; j++){
            intDifference[j]=Integer.parseInt(dataArray2[j])-Integer.parseInt(dataArray1[j]);
        }
        for(int j=0;j<intDifference.length;j++){
            stringDifference[j]=Integer.toString(intDifference[j]);
        }
        return stringDifference[index];
    }

    /**
     * find the distance between two fingers
     * @param event
     * @return
     */
    private float findDistance(MotionEvent event){
        float x=event.getX(0)-event.getX(1);
        float y=event.getY(0)-event.getX(1);
        float d=x*x+y*y;
        return (float) Math.sqrt(d);
    }

    /**
     * find the midpt between two fingers, used for Matrix.postScale()
     * @param point
     * @param event
     */
    private void findMidPoint(PointF point, MotionEvent event){
        float x=event.getX(0)+event.getX(1);
        float y=event.getY(0)+event.getY(1);
        point.set(x/2,y/2);
    }

    /**
     * Dragging and Zooming using imageview matrix scaleType
     * @param v which imageview
     * @param event
     * @return
     */
    public boolean onTouch(View v, MotionEvent event){
        switch(v.getId()){
            case R.id.photo1:
                switch(event.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN://first pointer down
                        savedMatrix1.set(matrix1);
                        start1.set(event.getX(), event.getY());
                        mode1=DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist1=findDistance(event);
                        if(oldDist1>10f){
                            savedMatrix1.set(matrix1);
                            findMidPoint(mid1,event);
                            mode1=ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                        mode1=NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(mode1==DRAG) {
                            matrix1.set(savedMatrix1);
                            float dx = event.getX() - start1.x;
                            float dy = event.getY() - start1.y;
                            matrix1.postTranslate(dx, dy);
                            break;
                        }else if (mode1==ZOOM){
                            float newDist=findDistance(event);
                            if(newDist>10f){
                                matrix1.set(savedMatrix1);
                                float scale=(newDist/oldDist1);
                                matrix1.postScale(scale,scale,mid1.x,mid1.y);
                            }
                        }
                }//switch
                photo1.setImageMatrix(matrix1);
                bmap1 = Bitmap.createBitmap(photo1.getWidth(), photo1.getHeight(), Bitmap.Config.RGB_565);
                Canvas canvas1 = new Canvas(bmap1);
                photo1.draw(canvas1);
                break;

            case R.id.photo2:
                switch(event.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN://first pointer down
                        savedMatrix2.set(matrix2);
                        start2.set(event.getX(), event.getY());
                        mode2=DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist2=findDistance(event);
                        if(oldDist2>10f){
                            savedMatrix2.set(matrix2);
                            findMidPoint(mid2,event);
                            mode2=ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                        mode2=NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(mode2==DRAG) {
                            matrix2.set(savedMatrix2);
                            float dx = event.getX() - start2.x;
                            float dy = event.getY() - start2.y;
                            matrix2.postTranslate(dx, dy);
                            break;
                        }else if (mode2==ZOOM){
                            float newDist=findDistance(event);
                            if(newDist>10f){
                                matrix2.set(savedMatrix2);
                                float scale=(newDist/oldDist2);
                                matrix2.postScale(scale,scale,mid2.x,mid2.y);
                            }
                        }
                }//switch
                photo2.setImageMatrix(matrix2);
                bmap2 = Bitmap.createBitmap(photo2.getWidth(), photo2.getHeight(), Bitmap.Config.RGB_565);
                Canvas canvas2 = new Canvas(bmap2);
                photo2.draw(canvas2);
        }//switch for different IDs

        return true;
    }

    /**
     * Get the Bitmap screenshot of a view
     * @param v
     * @return
     */
    public static Bitmap getBitmapOfView(View v){
        Bitmap b=Bitmap.createBitmap(v.getWidth(),
                v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);
        v.layout(v.getLeft(),v.getTop(),v.getRight(),v.getBottom());
        v.draw(c);
        return b;
    }
    /**
     *Get folder path
     * @return file path to a folder (not a file)
     */
    public File getPublicDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM),"collage");
        if (!file.mkdirs()) {
            Log.e("PUBLIC DIRECTORY", "Directory not created");
        }
        return file;
    }
}

