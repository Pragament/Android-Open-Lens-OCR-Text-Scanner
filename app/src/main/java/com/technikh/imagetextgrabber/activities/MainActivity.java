/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

package com.technikh.imagetextgrabber.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.models.VisionWordModel;
import com.technikh.imagetextgrabber.room.entity.Images;
import com.technikh.imagetextgrabber.widgets.MultiSelectSpinnerWidget;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity{

    int SELECT_PICTURE = 101;
    int SELECT_PDF = 102;
    TouchImageView ivImage;
    TextView saveNoteTV;
    RelativeLayout imageParentLayout;
    public EditText et_image_text;

    private SlidingUpPanelLayout mLayout;
    private String TAG = "MainActivity";
    private String PREF_SPINNER_USER_SETTINGS = "spinner_user_settings";
    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";

    //private FirebaseAnalytics mFirebaseAnalytics;
    ImageViewSettingsModel imageViewSettingsModel;
    public static com.technikh.imagetextgrabber.room.MyDatabase db;


    public static final String DBNAME="mydb";
    private com.technikh.imagetextgrabber.room.dao.HighlightDataAccess markerDao;
    private com.technikh.imagetextgrabber.room.dao.ImagesDataAccess imagesDao;
    public static String currentUri="default";
    public static java.util.List<MyVisionWordModel> savedRects=new ArrayList<>();
    private ArrayList<String> colorArray;
    private GridView gridView;
    private  AlertDialog alertDialog;
    public Integer recentHighlight=null;
    private Button btnSearchDictionary, btnSearchImage;
    private TextView tvDictionary;
    private ImageView ivRelatedImage;
    private RequestQueue requestQueue;


    public class MyVisionWordModel extends VisionWordModel{
        public String color;
        public String note;

        public MyVisionWordModel(Rect rect, String text, String color,String note) {
            super(rect, text);
            this.color=color;
            this.note=note;
        }


    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSearchDictionary = findViewById(R.id.btnSearchDictionary);
        btnSearchImage = findViewById(R.id.btnSearchImage);
        tvDictionary = findViewById(R.id.tvDictionary);

        ivRelatedImage = findViewById(R.id.ivRelatedImage);
        requestQueue = Volley.newRequestQueue(this); // Initialize Volley

        btnSearchDictionary.setOnClickListener(v -> {


            fetchDictionaryDefinition("ताज़ा"); // Example word
        });

        btnSearchImage.setOnClickListener(v -> {Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();


            fetchRelatedImage(""); // Example image search
        });



        //Toast.makeText(getApplicationContext(),Wiki.getTextExtract("Stack Overflow"),Toast.LENGTH_LONG).show();

        try
        {


            saveNoteTV = findViewById(R.id.save_note);
            saveNoteTV.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {


                    EditText et = findViewById(R.id.et);
                    String notes = et.getText().toString().trim();
                    if (notes.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Please enter note", Toast.LENGTH_LONG).show();
                    } else {
                        ivImage.saveNote(notes);
                        //ToDo:Addtoastinsavenotefunction
                        Toast.makeText(getApplicationContext(), "Note Saved", Toast.LENGTH_LONG).show();

                    }
                }
            });

            gridView = getLayoutInflater().inflate(R.layout.grid, null, false).findViewById(R.id.grid);
            alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setView(gridView)
                    .create();


            colorArray = new ArrayList<>();
            colorArray.add("#f6e58d");

            db = androidx.room.Room.databaseBuilder(getApplicationContext(),
                    com.technikh.imagetextgrabber.room.MyDatabase.class, DBNAME).build();


            markerDao = db.getHighlightsDao();
            imagesDao = db.getImagesDao();











        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
            //mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

            MultiSelectSpinnerWidget mySpin = (MultiSelectSpinnerWidget) findViewById(R.id.spinner_options);
            imageViewSettingsModel = new ImageViewSettingsModel();
            mySpin.setItems(imageViewSettingsModel.getAllItems());

            String savedString = sharedPref.getString(PREF_SPINNER_USER_SETTINGS, imageViewSettingsModel.getDefaultItemsString());
            String[] items = savedString.split(",");
            int[] savedList = new int[items.length];
            for (int i = 0; i < items.length; i++) {
                savedList[i] = Integer.parseInt(items[i]);
            }
            mySpin.setSelection(savedList);
            mySpin.refreshSpinner();
            imageViewSettingsModel.setSelectedItems(mySpin.getSelectedIndicies());
            mySpin.setOnMultiChoiceClickListener(new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    java.util.List<Integer> list = mySpin.getSelectedIndicies();
                    String delimitedString = TextUtils.join(",", list);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREF_SPINNER_USER_SETTINGS, delimitedString);
                    editor.commit();

                    imageViewSettingsModel.setSelectedItems(mySpin.getSelectedIndicies());
                    ivImage.initOptions(imageViewSettingsModel);

                    android.os.Bundle bundle = new android.os.Bundle();
                    //bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, delimitedString);
                    //mFirebaseAnalytics.logEvent("SPINNER_SETTINGS_CHANGE", bundle);
                }
            });

            ivImage = findViewById(R.id.ivImage);


            imageParentLayout = findViewById(R.id.rlParentWrapper);
            et_image_text = findViewById(R.id.et_image_text);
            mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
            mLayout.setAnchorPoint(0.7f);

            // Get intent, action and MIME type
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            boolean loadDefaultImage = true;

            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (imageUri != null) {
                        com.bumptech.glide.Glide.with(MainActivity.this)
                                .load(imageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivImage);
                        loadDefaultImage = false;
                    }
                } else if (type.startsWith("application/pdf")) {
                    Uri pdfUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    android.util.Log.d(TAG, "onCreate: pdfUri " + pdfUri);
                    ivImage.setVisibility(android.view.View.GONE);

                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("uri", pdfUri.toString());
                    startActivity(new Intent(MainActivity.this, PdfRendererBasicFragment.class)
                            .putExtra("bundle", args)
                    );
                }
            } else if (Intent.ACTION_VIEW.equals(action) && type != null) {
                android.util.Log.d(TAG, "onCreate: type " + type);
                android.os.Bundle bundle = intent.getExtras();
                android.util.Log.d(TAG, "onCreate: intent.getData() " + intent.getData());
                if (bundle != null) {
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
                        android.util.Log.d(TAG, String.format("%s %s (%s)", key,
                                value.toString(), value.getClass().getName()));
                    }
                }
                if (type.startsWith("image/")) {
                    Uri imageUri = (Uri) intent.getData();
                    if (imageUri != null) {
                        com.bumptech.glide.Glide.with(MainActivity.this)
                                .load(imageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivImage);
                        loadDefaultImage = false;
                    }
                } else if (type.startsWith("application/pdf")) {
                    Uri pdfUri = (Uri) intent.getData();
                    android.util.Log.d(TAG, "onCreate: pdfUri " + pdfUri);
                    ivImage.setVisibility(android.view.View.GONE);

                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("uri", pdfUri.toString());
                    startActivity(new Intent(MainActivity.this, PdfRendererBasicFragment.class)
                            .putExtra("bundle", args)
                    );


                }
            }
            if (loadDefaultImage) {
                com.bumptech.glide.Glide.with(MainActivity.this)
                        .load(Uri.parse("file:///android_asset/Example.png"))
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {

                                showSavedHighlights(resource);
                                return false;
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);
            }
            initImageView();


            com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    pickImage();
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                }
            });

            com.google.android.material.floatingactionbutton.FloatingActionButton fabPdf = findViewById(R.id.fabPdf);
            fabPdf.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        pickPdf();
                    } else {
                        android.os.Bundle bundle = new android.os.Bundle();
                        //mFirebaseAnalytics.logEvent("DEVICE_NO_SUPPORT_PDF", bundle);
                        Snackbar.make(view, "Your device version doesn't support our PDF opening library!", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            });
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();

        }
        catch (Throwable e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();

        }

    }



    // Fetch Dictionary Meaning
    private void fetchDictionaryDefinition(String word) {
        String url = "https://text.pollinations.ai/define%20in%20hindi%20and%20english%20" + word;


        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    tvDictionary.setText(response);
                    tvDictionary.setVisibility(View.VISIBLE);

                },
                error -> Toast.makeText(MainActivity.this, "Error fetching dictionary", Toast.LENGTH_SHORT).show());

        requestQueue.add(stringRequest);
    }

    // Fetch Related Image
    private void fetchRelatedImage(String word) {
        String imageUrl = "https://image.pollinations.ai/prompt/image%20representing%20" + word;

        ImageRequest imageRequest = new ImageRequest(imageUrl,
                response -> {
                    ivRelatedImage.setImageBitmap(response);
                    ivRelatedImage.setVisibility(View.VISIBLE);

                },
                0, 0, ImageView.ScaleType.CENTER_CROP, null,
                error -> Toast.makeText(MainActivity.this, "Error fetching image", Toast.LENGTH_SHORT).show());

        requestQueue.add(imageRequest);
    }






    private void initImageView(){
        ivImage.initOptions(imageViewSettingsModel);

        ivImage.setCustomEventListener(new TouchImageView.OnCustomEventListener() {
            public void onEvent() {
                et_image_text.setText(ivImage.getContentDescription());
                et_image_text.setSelectAllOnFocus(true);
            }
        });
        et_image_text.setText("");
    }

    public void pickPdf() {

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), SELECT_PDF);

    }

    public void pickImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);

    }

    //------------------------Updated Code---------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            currentUri = data.getData().toString();

            if (requestCode == SELECT_PICTURE) {
                handleImageSelection(data);
            } else if (requestCode == SELECT_PDF) {
                handlePdfSelection(data);
            }
        }
    }

    private void handleImageSelection(Intent data) {
        try {
            ViewGroup.LayoutParams lParams = ivImage.getLayoutParams();
            imageParentLayout.removeView(ivImage);
            ivImage = new TouchImageView(this);
            ivImage.setLayoutParams(lParams);
            imageParentLayout.addView(ivImage);

            ivImage.setVisibility(View.VISIBLE);
            findViewById(R.id.container).setVisibility(View.GONE);

            com.bumptech.glide.Glide.with(MainActivity.this)
                    .asBitmap()
                    .load(data.getData())
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, com.bumptech.glide.request.target.Target<Bitmap> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image", e);
                            Toast.makeText(MainActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, com.bumptech.glide.request.target.Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            try {
                                Drawable drawable = new BitmapDrawable(getResources(), resource);
                                showSavedHighlights(drawable);

                                // Delay text recognition to ensure image is fully loaded
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    recognizeText(resource);
                                }, 500);

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing image: " + e.getMessage(), e);
                                Toast.makeText(MainActivity.this, "Error processing image", Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImage);

            initImageView();

            Bundle bundle = new Bundle();
            //mFirebaseAnalytics.logEvent("IMAGE_CHANGE", bundle);

        } catch (Exception e) {
            Log.e(TAG, "Error handling image selection: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePdfSelection(Intent data) {
        try {
            Bundle bundle = new Bundle();
            //mFirebaseAnalytics.logEvent("PDF_CHANGE", bundle);

            ivImage.setVisibility(View.VISIBLE);
            findViewById(R.id.container).setVisibility(View.GONE);

            // Show progress dialog
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle("Loading PDF")
                    .setMessage("Please wait while PDF is being loaded...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            // Process PDF in background thread to prevent UI blocking
            new Thread(() -> {
                ParcelFileDescriptor pfd = null;
                PdfRenderer renderer = null;
                PdfRenderer.Page page = null;

                try {
                    pfd = getContentResolver().openFileDescriptor(data.getData(), "r");
                    if (pfd != null) {
                        renderer = new PdfRenderer(pfd);
                        page = renderer.openPage(0);

                        // Get original dimensions
                        int originalWidth = page.getWidth();
                        int originalHeight = page.getHeight();

                        // Calculate optimal bitmap size to prevent memory issues
                        int maxSize = 2048;
                        float scale = 1.0f;

                        if (originalWidth > maxSize || originalHeight > maxSize) {
                            scale = Math.min((float)maxSize / originalWidth, (float)maxSize / originalHeight);
                        }

                        int scaledWidth = (int)(originalWidth * scale);
                        int scaledHeight = (int)(originalHeight * scale);

                        // Create bitmap with calculated size
                        Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(Color.WHITE); // Fill with white background

                        // Create matrix for scaling if needed
                        android.graphics.Matrix matrix = null;
                        if (scale != 1.0f) {
                            matrix = new android.graphics.Matrix();
                            matrix.setScale(scale, scale);
                        }

                        // Render page to bitmap
                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            try {
                                progressDialog.dismiss();

                                // Set bitmap to ImageView
                                ivImage.setImageBitmap(bitmap);

                                // Initialize ImageView for PDF interaction
                                initImageView();

                                // Show saved highlights if any
                                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                                showSavedHighlights(drawable);

                                // Delay text recognition to ensure PDF is fully displayed
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (!bitmap.isRecycled()) {
                                        recognizeText(bitmap);
                                    }
                                }, 1000);

                                Toast.makeText(MainActivity.this, "PDF loaded successfully", Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI with PDF: " + e.getMessage(), e);
                                Toast.makeText(MainActivity.this, "Error displaying PDF", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Unable to open PDF file", Toast.LENGTH_SHORT).show();
                        });
                    }

                } catch (IOException e) {
                    Log.e(TAG, "IOException while processing PDF: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Failed to load PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException while processing PDF: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Permission denied to access PDF", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error while processing PDF: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Unexpected error loading PDF", Toast.LENGTH_SHORT).show();
                    });
                } finally {
                    // Clean up resources
                    try {
                        if (page != null) {
                            page.close();
                        }
                        if (renderer != null) {
                            renderer.close();
                        }
                        if (pfd != null) {
                            pfd.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing PDF resources: " + e.getMessage(), e);
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error handling PDF selection: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
            requestQueue.stop();
        }

        // Clean up any remaining bitmaps
        if (ivImage != null && ivImage.getDrawable() != null) {
            Drawable drawable = ivImage.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }

    // Add this method to handle low memory situations
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            // Clear image caches when memory is low
            com.bumptech.glide.Glide.get(this).clearMemory();
        }
    }



    // text recognition methods

    private void recognizeText(Bitmap bitmap) {
     //   recognizeTextWithModels(bitmap);
        showLanguageSelectionDialog(bitmap);
    }



    // show dialog for language selection

    private void showLanguageSelectionDialog(Bitmap bitmap) {
        String[] languageNames = {
                "English", "Hindi", "Marathi", "Tamil", "Telugu", "Gujarati", "Bengali", "Punjabi", "Kannada", "Malayalam",
                "Odia", "Urdu", "Arabic", "Chinese", "Japanese", "Korean", "Thai", "French", "German", "Italian", "Spanish", "Russian"
        };
        String[] languageCodes = {
                "en", "hi", "mr", "ta", "te", "gu", "bn", "pa", "kn", "ml", "or", "ur", "ar", "zh", "ja", "ko", "th", "fr", "de", "it", "es", "ru"
        };

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languageNames);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        // Load downloaded models from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MLKitModels", MODE_PRIVATE);
        Set<String> downloadedModels = prefs.getStringSet("DownloadedModels", new HashSet<>());

        TextView downloadedModelsText = new TextView(this);
        downloadedModelsText.setText("Downloaded Models:");
        layout.addView(downloadedModelsText);

        LinearLayout downloadedModelsLayout = new LinearLayout(this);
        downloadedModelsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(downloadedModelsLayout);

        if (downloadedModels.isEmpty()) {
            downloadedModelsText.setText("No downloaded models.");
        } else {
            for (String modelName : downloadedModels) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView modelTextView = new TextView(this);
                modelTextView.setText(modelName);
                row.addView(modelTextView);


                downloadedModelsLayout.addView(row);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Language for OCR")
                .setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    int position = spinner.getSelectedItemPosition();
                    String selectedLanguageCode = languageCodes[position];
                    addModelToStorage(languageNames[position]); // Track model
                    recognizeTextWithMLKit(bitmap, selectedLanguageCode);
                })

                .show();
    }

    // add model to storage

    private void addModelToStorage(String modelName) {
        SharedPreferences prefs = getSharedPreferences("MLKitModels", MODE_PRIVATE);
        Set<String> models = new HashSet<>(prefs.getStringSet("DownloadedModels", new HashSet<>()));
        models.add(modelName);
        prefs.edit().putStringSet("DownloadedModels", models).apply();
    }




    // method for text recognition with MLKit

    private void recognizeTextWithMLKit(Bitmap bitmap, String languageCode) {
        TextRecognizer recognizer = getTextRecognizer(languageCode);

        if (recognizer == null) {
            Toast.makeText(this, "Text recognition model for " + languageCode + " is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Processing Image")
                .setMessage("Please wait while text recognition is in progress...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Delay processing by 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        progressDialog.dismiss();

                        if (visionText.getText().isEmpty()) {
                            Toast.makeText(this, "No text found in image", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        StringBuilder recognizedText = new StringBuilder();
                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            recognizedText.append(block.getText()).append("\n");
                        }

                        EditText editText = findViewById(R.id.recognized_text_edit);
                        Button okButton = findViewById(R.id.ok_button);

                        editText.setText(recognizedText.toString());
                        editText.setVisibility(View.VISIBLE);
                        okButton.setVisibility(View.VISIBLE);

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                                .setTitle("Recognized Text")
                                .setMessage(visionText.getText())
                                .setPositiveButton("OK", (dialog, which) -> {
                                    dialog.dismiss();
                                    editText.setVisibility(View.GONE);
                                    okButton.setVisibility(View.GONE);
                                });

                        okButton.setOnClickListener(v -> alertDialog.show());
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to recognize text. Please try again later.", Toast.LENGTH_LONG).show();
                    });
        }, 5000); // 5-second delay
    }



    // method to get text recognizer based on language code
    private TextRecognizer getTextRecognizer(String languageCode) {
        switch (languageCode) {
            case "zh":
                return TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            case "ja":
                return TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
            case "ko":
                return TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            case "hi": case "mr": case "bn": case "pa":
            case "ta": case "te": case "gu": case "kn":
            case "ml": case "or": case "ur":
                return TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
            default:
                return TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
        }
    }










    //----------------------------------------------------------------------

    public void showSavedHighlights(Drawable drawable){
        //get last read image
        //...


        //show highlights on image
        new Thread(){
            @Override
            public void run() {
                savedRects.clear();
                for(Images imageInfo:imagesDao.getAllImage(currentUri)){
                    Rect rect=new Rect(imageInfo.left,imageInfo.top,imageInfo.right,imageInfo.bottom);
                    MyVisionWordModel visionWordModel=new MyVisionWordModel(rect,imageInfo.text,imageInfo.color,imageInfo.note);
                    savedRects.add(visionWordModel);

                }

                //draw on image with color


                //canvas.drawBitmap(originalBitmap, 0, 0, paint);
                //canvas.drawText("Testing...", 10, 10, paint);

                final Bitmap originalBitmap;
        /*if(longPressMode){
            originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
        }else {*/
                originalBitmap = ((BitmapDrawable) drawable).getBitmap();
                //}
                Canvas canvas = new Canvas(originalBitmap);

                for(int i=0;i<savedRects.size();++i) {
                    MyVisionWordModel visionWordModel=savedRects.get(i);
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.FILL);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.parseColor(visionWordModel.color));
                    paint.setAntiAlias(true);
                    int finalI = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canvas.drawRect(visionWordModel.mrect, paint);

                            if(finalI ==savedRects.size()-1){
                                ivImage.invalidate();
                            }
                        }
                    });


                }


            }
        }.start();
    }


    public void highlightSelected(View v){
        if(((ImageView)v).getDrawable()!=null){



            //remove highlight and x
            recentHighlight=v.getId();
            ImageView iv=findViewById(recentHighlight);
            iv.setImageDrawable(null);

            ivImage.highlight("#00000000");
            ivImage.invalidate();

        }
        else {
            //add highlight and remove x from previous marker
            if(recentHighlight!=null){
                ImageView iv=findViewById(recentHighlight);
                iv.setImageDrawable(null);
            }

            recentHighlight=v.getId();
            ImageView iv=findViewById(recentHighlight);
            iv.setImageDrawable(getResources().getDrawable(R.drawable.ic_clear));

            ivImage.highlight(v.getTag().toString());
            ivImage.invalidate();
        }

    }


    class MyGridView extends ArrayAdapter {


        public MyGridView(Context c){
            super(c, android.R.layout.simple_list_item_1,colorArray);
        }


        @androidx.annotation.NonNull
        @Override
        public android.view.View getView(int position, @androidx.annotation.Nullable android.view.View convertView, @androidx.annotation.NonNull ViewGroup parent) {
            convertView=getLayoutInflater().inflate(R.layout.marker,null,false);
            android.widget.TextView tv=convertView.findViewById(R.id.tv);
            tv.setBackgroundColor(Color.parseColor(colorArray.get(position)));
            tv.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    ivImage.highlight(colorArray.get(position));
                }
            });
            return convertView;
        }
    }
}



