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


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.common.model.CustomRemoteModel;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.models.VisionWordModel;
import com.technikh.imagetextgrabber.room.entity.Images;
import com.technikh.imagetextgrabber.widgets.MultiSelectSpinnerWidget;
import com.technikh.imagetextgrabber.widgets.TouchImageView;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import android.util.Log;

public class MainActivity extends AppCompatActivity{

    int SELECT_PICTURE = 101;
    int SELECT_PDF = 102;
    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final String STATE_PDF_URI = "pdf_uri";
    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    private int mPageCount;
    private int mCurrentPageIndex = 0;
    private SparseArray<Bitmap> mPageCache = new SparseArray<>();
    private final int CACHE_SIZE = 3;
    private ExecutorService mPdfExecutor = Executors.newSingleThreadExecutor();
    private Uri mPdfUri;
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
        Button btnPrevPage = findViewById(R.id.btnPrevPage);
        Button btnNextPage = findViewById(R.id.btnNextPage);
        TextView tvPageInfo = findViewById(R.id.tvPageInfo);
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

    // Add these constants at the top of the class
    private static final int MAX_CACHE_SIZE = 5; // Reduced from unlimited
    private static final int MAX_BITMAP_DIMENSION = 2048; // Max width/height for bitmaps

    private void initImageViewForPdf() {
        // Re-initialize the custom event listener for text copying
        ivImage.setCustomEventListener(new TouchImageView.OnCustomEventListener() {
            public void onEvent() {
                et_image_text.setText(ivImage.getContentDescription());
                et_image_text.setSelectAllOnFocus(true);
            }

        });
        addGestureSupport();
        // Initialize options
        ivImage.initOptions(imageViewSettingsModel);

        // FIXED: Reset the ImageView to ensure it's in a proper state for interaction
        // Check if the method exists before calling it
        try {
            // Try to call resetZoom if it exists
            ivImage.getClass().getMethod("resetZoom").invoke(ivImage);
        } catch (Exception e) {
            // If resetZoom doesn't exist, try alternative methods
            try {
                // Try setZoom method with scale 1.0f
                ivImage.getClass().getMethod("setZoom", float.class).invoke(ivImage, 1.0f);
            } catch (Exception e2) {
                // If no zoom methods exist, just log and continue
                Log.d(TAG, "No zoom reset methods available in TouchImageView");
            }
        }
    }

    // Replace the renderPage method with this optimized version
    private void renderPage(int pageIndex) {
        if (mPdfRenderer == null || pageIndex < 0 || pageIndex >= mPageCount) {
            Log.e(TAG, "Invalid renderPage call: pageIndex=" + pageIndex + ", mPageCount=" + mPageCount);
            return;
        }

        Log.d(TAG, "Rendering page: " + pageIndex);
        showProgress(true);

        // Try to get from cache first
        Bitmap cachedBitmap = mPageCache.get(pageIndex);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            Log.d(TAG, "Using cached bitmap for page " + pageIndex);
            ivImage.setImageBitmap(cachedBitmap);
            mCurrentPageIndex = pageIndex;
            updatePageInfo();
            showProgress(false);
            // Re-initialize TouchImageView functionality for cached pages
            initImageViewForPdf();
            return;
        }

        // Close current page if exists
        if (mCurrentPage != null) {
            mCurrentPage.close();
            mCurrentPage = null;
        }

        mPdfExecutor.execute(() -> {
            try {
                Log.d(TAG, "Rendering page in background: " + pageIndex);

                // Open the requested page
                PdfRenderer.Page page = mPdfRenderer.openPage(pageIndex);

                // Calculate scaled dimensions that fit within MAX_BITMAP_DIMENSION
                float scale = Math.min(
                        (float)MAX_BITMAP_DIMENSION / page.getWidth(),
                        (float)MAX_BITMAP_DIMENSION / page.getHeight()
                );

                int width = (int)(page.getWidth() * scale);
                int height = (int)(page.getHeight() * scale);

                // Create bitmap with calculated dimensions
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE); // Fill with white background

                // Render the page
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                Log.d(TAG, "Page rendered successfully: " + pageIndex);

                // Update cache on UI thread
                runOnUiThread(() -> {
                    try {
                        // Add to cache and enforce size limit
                        mPageCache.put(pageIndex, bitmap);
                        trimCache();

                        // Display the page
                        ivImage.setImageBitmap(bitmap);
                        mCurrentPageIndex = pageIndex;
                        mCurrentPage = null; // Page is already closed

                        updatePageInfo();
                        showProgress(false);

                        // Re-initialize TouchImageView functionality and run text recognition
                        initImageViewForPdf();
                        recognizeText(bitmap); // Add text recognition for each page

                        Log.d(TAG, "Page displayed successfully: " + pageIndex);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI with rendered page", e);
                        showProgress(false);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error rendering page " + pageIndex, e);
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(this, "Error rendering page " + (pageIndex + 1), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Add this method to manage cache size
    private void trimCache() {
        while (mPageCache.size() > MAX_CACHE_SIZE) {
            int oldestKey = mPageCache.keyAt(0);
            Bitmap oldest = mPageCache.get(oldestKey);
            if (oldest != null && !oldest.isRecycled()) {
                oldest.recycle();
            }
            mPageCache.remove(oldestKey);
        }
    }

    // Update the closePdfRenderer method
    private void closePdfRenderer() {
        mPdfExecutor.execute(() -> {
            if (mCurrentPage != null) {
                mCurrentPage.close();
                mCurrentPage = null;
            }

            if (mPdfRenderer != null) {
                mPdfRenderer.close();
                mPdfRenderer = null;
            }

            // Clear cache on background thread
            for (int i = 0; i < mPageCache.size(); i++) {
                Bitmap bitmap = mPageCache.valueAt(i);
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            mPageCache.clear();
        });
    }
    private void setupNavigationControls() {
        // Find or create navigation buttons
        Button btnPrevPage = findViewById(R.id.btnPrevPage);
        Button btnNextPage = findViewById(R.id.btnNextPage);
        TextView tvPageInfo = findViewById(R.id.tvPageInfo);

        // Update page info display
        updatePageInfo();

        // Previous page button
        btnPrevPage.setOnClickListener(v -> {
            if (mCurrentPageIndex > 0) {
                renderPage(mCurrentPageIndex - 1);
                updatePageInfo();
            }
        });

        // Next page button
        btnNextPage.setOnClickListener(v -> {
            if (mCurrentPageIndex < mPageCount - 1) {
                renderPage(mCurrentPageIndex + 1);
                updatePageInfo();
            }
        });

        // Enable/disable buttons based on current page
        btnPrevPage.setEnabled(mCurrentPageIndex > 0);
        btnNextPage.setEnabled(mCurrentPageIndex < mPageCount - 1);
    }

    private void updatePageInfo() {
        TextView tvPageInfo = findViewById(R.id.tvPageInfo);
        if (tvPageInfo != null) {
            tvPageInfo.setText(String.format("Page %d of %d", mCurrentPageIndex + 1, mPageCount));
        }

        // Update button states
        Button btnPrevPage = findViewById(R.id.btnPrevPage);
        Button btnNextPage = findViewById(R.id.btnNextPage);

        if (btnPrevPage != null) {
            btnPrevPage.setEnabled(mCurrentPageIndex > 0);
        }
        if (btnNextPage != null) {
            btnNextPage.setEnabled(mCurrentPageIndex < mPageCount - 1);
        }
    }

    // Update the loadPdf method
    private void loadPdf(Uri pdfUri) {
        ivImage.setVisibility(View.VISIBLE);
        findViewById(R.id.container).setVisibility(View.GONE);
        showProgress(true);

        mPdfExecutor.execute(() -> {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(pdfUri, "r");
                if (pfd != null) {
                    closePdfRenderer(); // Close any existing renderer first

                    PdfRenderer renderer = new PdfRenderer(pfd);
                    int pageCount = renderer.getPageCount();

                    runOnUiThread(() -> {
                        mPdfRenderer = renderer;
                        mPageCount = pageCount;
                        mPdfUri = pdfUri;
                        mCurrentPageIndex = 0; // Reset to first page

                        // Setup navigation controls first
                        setupNavigationControls();

                        // Show navigation controls
                        View navLayout = findViewById(R.id.pdfNavigationLayout);
                        if (navLayout != null) {
                            navLayout.setVisibility(View.VISIBLE);
                        }

                        // Initialize TouchImageView for PDF mode
                        initImageViewForPdf();
                        addGestureSupport(); // Re-add gesture support for PDF mode

                        // Load first page after setup
                        renderPage(0);
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addGestureSupport() {
        ivImage.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            private boolean isPdfMode = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Check if we're in PDF mode
                isPdfMode = (mPdfRenderer != null && mPageCount > 0);

                if (!isPdfMode) {
                    // For regular images, let TouchImageView handle all touch events
                    return false; // This allows TouchImageView's built-in functionality
                }

                // For PDF mode, handle swipe gestures but allow zoom/pan for other gestures
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;

                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();

                        float deltaX = endX - startX;
                        float deltaY = endY - startY;

                        // Only handle horizontal swipes that are significantly larger than vertical movement
                        if (Math.abs(deltaX) > Math.abs(deltaY) &&
                                Math.abs(deltaX) > SWIPE_THRESHOLD &&
                                Math.abs(deltaY) < 50) { // Small vertical threshold to avoid conflicts

                            if (deltaX > 0) {
                                // Swipe right - previous page
                                if (mCurrentPageIndex > 0) {
                                    renderPage(mCurrentPageIndex - 1);
                                    return true; // Consume the event
                                }
                            } else {
                                // Swipe left - next page
                                if (mCurrentPageIndex < mPageCount - 1) {
                                    renderPage(mCurrentPageIndex + 1);
                                    return true; // Consume the event
                                }
                            }
                        }
                        break;
                }

                // For PDF mode, let TouchImageView handle zoom/pan for non-swipe gestures
                return false;
            }
        });
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mCurrentPageIndex);
        outState.putParcelable(STATE_PDF_URI, mPdfUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE, 0);
        mPdfUri = savedInstanceState.getParcelable(STATE_PDF_URI);
        if (mPdfUri != null) {
            loadPdf(mPdfUri);
        }
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
        if (resultCode == RESULT_OK) {
            currentUri = data.getData().toString();
            if (requestCode == SELECT_PICTURE) {
                ViewGroup.LayoutParams lParams = ivImage.getLayoutParams();
                imageParentLayout.removeView(ivImage);
                ivImage = new TouchImageView(this);
                ivImage.setLayoutParams(lParams);
                imageParentLayout.addView(ivImage);

                ivImage.setVisibility(android.view.View.VISIBLE);
                findViewById(R.id.container).setVisibility(android.view.View.GONE);

                com.bumptech.glide.Glide.with(MainActivity.this)
                        .asBitmap()
                        .load(data.getData())
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, com.bumptech.glide.request.target.Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, com.bumptech.glide.request.target.Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                Drawable drawable = new BitmapDrawable(getResources(), resource);
                                showSavedHighlights(drawable);
                                recognizeText(resource);
                                return false;
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);

                initImageView();

                android.os.Bundle bundle = new android.os.Bundle();
                //mFirebaseAnalytics.logEvent("IMAGE_CHANGE", bundle);
            }else if (requestCode == SELECT_PDF) {
                mPdfUri = data.getData();
                currentUri = data.getData().toString(); // Set current URI for PDF

                // Hide navigation layout initially
                View navLayout = findViewById(R.id.pdfNavigationLayout);
                if (navLayout != null) {
                    navLayout.setVisibility(View.GONE);
                }

                showProgress(true);
                loadPdf(mPdfUri);



                try {
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "r");
                    if (pfd != null) {
                        PdfRenderer renderer = new PdfRenderer(pfd);
                        PdfRenderer.Page page = renderer.openPage(0);

                        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        recognizeText(bitmap);



                        // PDF ka preview set karte hain
                        ivImage.setImageBitmap(bitmap);

                        page.close();
                        renderer.close();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
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



