package com.example.advanceandroidnotesapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceandroidnotesapp.R;
import com.example.advanceandroidnotesapp.database.NotesDatabase;
import com.example.advanceandroidnotesapp.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime;
    private View viewSubtitleIndicator;
    private ImageView imageNote;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;

    private String selectedNoteColor;
    private String seletedImagePath;

    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;
    private int textAlignment = View.TEXT_ALIGNMENT_TEXT_START;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;

    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Thiết lập giao diện và ánh xạ các thành phần giao diện:
        setContentView(R.layout.activity_create_note);

        // Thiết lập sự kiện khi người dùng bấm nút back (imageBack), khi đó sẽ gọi onBackPressed() để thoát khỏi hoạt động hiện tại.
        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imageNote = findViewById(R.id.imageNote);
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);

        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()) // Saturday, 13 June 2020 21:09 PM
                        .format(new Date())
        );

        // Thiết lập sự kiện khi người dùng bấm nút lưu (imageSave), khi đó sẽ gọi phương thức saveNote().
        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        selectedNoteColor = "#333333";
        seletedImagePath = "";

        // Nếu isViewOrUpdate được truyền từ intent và là true,
        // lấy thông tin ghi chú đã có sẵn và gọi hàm setViewOrUpdateNote() để cập nhật giao diện.
        if(getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveWebURL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textWebURL.setText(null);
                layoutWebURL.setVisibility(View.GONE);
            }
        });

        // Thiết lập sự kiện khi người dùng bấm nút xóa hình ảnh (imageRemoveImage) hoặc xóa URL (imageRemoveWebURL),
        // sẽ xóa dữ liệu tương ứng và ẩn đi các thành phần liên quan trên giao diện.
        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                seletedImagePath = "";
            }
        });

        // Xử lý trường hợp ghi chú được tạo từ Quick Action.
        // Nếu isFromQuickAction được truyền từ intent và là true, kiểm tra loại hành động (quickActionType)
        // và hiển thị hình ảnh hoặc URL tương ứng.
        if(getIntent().getBooleanExtra("isFromQuickAction", false)){
            String type = getIntent().getStringExtra("quickActionType");
            if(type!=null){
                if(type.equals("image")){
                    seletedImagePath = getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(seletedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }else if(type.equals("URL")){
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }

        // Gọi hàm initMiscellaneous() để khởi tạo và cài đặt các phần tử phụ thuộc vào giao diện khác như màu sắc, thẻ ghi chú, v.v.
        initMiscellaneous();
        // Gọi setSubtitleIndicatorColor() để thiết lập màu sắc cho phần chỉ mục của phụ đề (ví dụ: một chấm màu) dựa trên màu sắc đã chọn.
        setSubtitleIndicatorColor();
    }

    // Đoạn code này thực hiện việc cập nhật giao diện người dùng để hiển thị thông tin của một ghi chú đã có sẵn (alreadyAvailableNote).
    private void setViewOrUpdateNote() {
        // Các dòng code này gán các giá trị từ alreadyAvailableNote (tiêu đề, phụ đề, nội dung ghi chú và thời gian)
        // cho các phần tử giao diện người dùng tương ứng.
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDatetime());
        if(alreadyAvailableNote.getImagePath()!= null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            // Hiển thị hình ảnh từ đường dẫn lưu trữ trong ghi chú
            // và hiển thị phần tử giao diện để xóa hình ảnh (nếu người dùng muốn xóa)
            // Gán đường dẫn hình ảnh đã chọn vào biến seletedImagePath
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            seletedImagePath = alreadyAvailableNote.getImagePath();
        }

        if(alreadyAvailableNote.getWebLink()!=null && !alreadyAvailableNote.getWebLink().trim().isEmpty()){
            // Hiển thị URL từ ghi chú và phần tử giao diện để xem URL
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
        // Đoạn code này gán màu được chọn từ alreadyAvailableNote vào selectedNoteColor và
        // sau đó cập nhật màu sắc cho phần hiển thị màu sắc (ví dụ: một chấm màu) trên giao diện bằng hàm setSubtitleIndicatorColor().
        //add
        selectedNoteColor=alreadyAvailableNote.getColor();
        setSubtitleIndicatorColor();
        // end

    }

    // Đoạn mã này thực hiện việc lưu ghi chú mới hoặc cập nhật ghi chú đã tồn tại vào cơ sở dữ liệu.
    // Đầu tiên, nó kiểm tra xem các trường dữ liệu chính của ghi chú như tiêu đề, phụ đề và nội dung ghi chú có rỗng không.
    // Nếu tiêu đề trống, hoặc cả phụ đề và nội dung ghi chú đều trống,
    // nó sẽ hiển thị thông báo tương ứng và không tiếp tục với việc lưu ghi chú.
    private void saveNote() {
        if(inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }else if(inputNoteSubtitle.getText().toString().trim().isEmpty()
            && inputNoteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty!",Toast.LENGTH_SHORT).show();
            return;
        }

        // Sau đó, nó tạo một đối tượng Note mới và thiết lập các thuộc tính của nó từ các trường dữ liệu
        // đã nhập hoặc được chọn trên giao diện người dùng.
        // Đây bao gồm tiêu đề, phụ đề, nội dung ghi chú, ngày giờ, màu sắc, đường dẫn hình ảnh và đường dẫn liên kết web nếu có.
        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDatetime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(seletedImagePath);

        if(layoutWebURL.getVisibility() == View.VISIBLE){
            note.setWebLink(textWebURL.getText().toString());
        }

        // Nếu ghi chú đã tồn tại (alreadyAvailableNote != null),
        // nó sẽ thiết lập ID cho ghi chú mới tương ứng với ID của ghi chú đã tồn tại để đảm bảo rằng nó được cập nhật thay vì tạo mới.
        if(alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }

        // Tiếp theo, để tránh thực hiện thao tác cơ sở dữ liệu trên luồng chính (Main Thread) -
        // điều Room Database không cho phép vì có thể gây chậm trễ hoặc đóng ứng dụng,
        // nó sử dụng một AsyncTask để thực hiện việc lưu ghi chú.
        // Room doesn't allow database operation on the Main thread. That's why we are using async task to save note
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {

            // doInBackground: Thực hiện việc chèn (insert) hoặc cập nhật (update) ghi chú trong cơ sở dữ liệu thông qua NoteDao của Room Database.
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            // onPostExecute: Được gọi sau khi doInBackground hoàn thành, nó trả kết quả về cho onPostExecute và đóng màn hình tạo ghi chú.
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
        //Nó tạo một AsyncTask tạm thời bên trong phương thức saveNote
        // để thực hiện tác vụ lưu ghi chú trong background và đảm bảo rằng việc lưu dữ liệu
        // không làm ảnh hưởng đến hiệu suất của giao diện người dùng.
    }

    // Phương thức này được gọi để khởi tạo và cấu hình các tuỳ chọn phụ, trong đó bao gồm:
    private void initMiscellaneous() {
        // Là layout chứa các tuỳ chọn phụ như chọn màu sắc, định dạng chữ, v.v.
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        // Được sử dụng để điều khiển hành vi của bottom sheet.
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        // textMiscellaneous: Là một phần tử trong layout, khi người dùng nhấn vào nó,
        // bottom sheet sẽ mở ra hoặc thu lại tùy thuộc vào trạng thái hiện tại.
        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView imageColor1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutMiscellaneous.findViewById(R.id.imageColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            // Mỗi khi người dùng chọn một màu sắc (thông qua onClick),
            // giá trị của biến selectedNoteColor được cập nhật tương ứng với màu sắc đã chọn.
            // Đồng thời, hình ảnh ic_done sẽ được hiển thị tại phần tử đã chọn để đánh dấu màu sắc đã được chọn,
            // và các hình ảnh đánh dấu trên các màu sắc khác sẽ bị ẩn đi (setImageResource(0)).
            public void onClick(View v) {
                selectedNoteColor = "#333333";
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                // Phương thức setSubtitleIndicatorColor() được gọi sau khi chọn một màu sắc để cập nhật màu sắc cho
                // phần indicator (chỉ thị) của ghi chú.
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#FDBE3B";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_done);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#FF4842";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_done);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#3A52FC";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_done);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#000000";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_done);
                setSubtitleIndicatorColor();
            }
        });

        // Đoạn code này liên kết một hành động khi người dùng nhấn vào một phần tử trên giao diện người dùng, được xác định bằng ID layoutAddImage.
        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Điều này giúp thu gọn bottom sheet (nếu nó đang mở ra) sau khi người dùng nhấn vào phần tử layoutAddImage.
                bottomSheetBehavior.setState(bottomSheetBehavior.STATE_COLLAPSED);
                // Kiểm tra xem ứng dụng có quyền truy cập vào bộ nhớ ngoại vi hay không.
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    // Nếu quyền truy cập chưa được cấp, yêu cầu cấp quyền truy cập bộ nhớ ngoại vi từ người dùng thông qua
                    // một hộp thoại cần phải xác nhận.
                    ActivityCompat.requestPermissions(
                            CreateNoteActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                }else {
                    // Nếu quyền truy cập đã được cấp, hàm selectImage() sẽ được gọi để cho phép người dùng chọn một hình ảnh từ bộ nhớ ngoại vi.
                    selectImage();
                }
            }
        });

        // Đoạn mã này kiểm tra xem nếu alreadyAvailableNote không null và thuộc tính màu sắc của nó không phải là null
        // và không chứa khoảng trắng khi được cắt bỏ (trim()), tức là không phải là chuỗi rỗng,
        // thì thực hiện một loạt các câu lệnh để chọn màu sắc đã lưu trữ trước đó và thiết lập nó lại khi tạo hoặc cập nhật ghi chú.

        // ếu alreadyAvailableNote không null và thuộc tính màu sắc của nó đã được lưu trữ (getColor()),
        // thì nó sẽ tiến hành kiểm tra chuỗi màu sắc đã lưu trữ đó.
        if(alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && alreadyAvailableNote.getColor().trim().isEmpty()){
            // Sử dụng câu lệnh switch-case để so sánh chuỗi màu sắc đã lưu trữ với các giá trị màu sắc đã được định nghĩa trước đó.
            switch (alreadyAvailableNote.getColor()){
                // Nếu màu sắc đã lưu trữ trước đó khớp với một trong các giá trị màu được xác định trong case:
                // Nếu là "#FDBE3B", phần tử viewColor2 trong layout layoutMiscellaneous sẽ nhận sự kiện click (performClick()),
                // tức là tự động thiết lập màu sắc này.
                case "#FDBE3B":
                    layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52FC":
                    layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hành động này giúp thu gọn bottom sheet (nếu nó đang mở ra) sau khi người dùng nhấn vào phần tử layoutAddUrl.
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                // Đây là hàm được gọi để hiển thị một hộp thoại (dialog) để người dùng có thể nhập một URL mới.
                showAddURLDialog();
            }
        });

        // Đoạn code này kiểm tra xem nếu có một ghi chú đã có sẵn (alreadyAvailableNote không null), nó sẽ thực hiện các hành động sau:
        if(alreadyAvailableNote!= null) {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Hành động này thu gọn bottom sheet (nếu nó đang mở ra) sau khi người dùng nhấn vào phần tử layoutDeleteNote.
                    bottomSheetBehavior.setState(bottomSheetBehavior.STATE_COLLAPSED);
                    // Đây là hàm được gọi để hiển thị một hộp thoại (dialog) cho phép người dùng xác nhận việc xóa ghi chú hiện tại.
                    showDeleteNoteDialog();
                }
            });
        }

        layoutMiscellaneous.findViewById(R.id.imageBold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBold = !isBold;
                setBold();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageItalic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isItalic = !isItalic;
                setItalic();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageUnderline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUnderline = !isUnderline;
                setUnderline();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageAlignmentLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START;
                setTextAlignment();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageAlignmentCenter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textAlignment = View.TEXT_ALIGNMENT_CENTER;
                setTextAlignment();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageAlignmentRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textAlignment = View.TEXT_ALIGNMENT_TEXT_END;
                setTextAlignment();
            }
        });
    }

    // showDeleteNoteDialog(): Phương thức này tạo và hiển thị hộp thoại xóa ghi chú.
    private void showDeleteNoteDialog() {
        // Nếu dialogDeleteNote chưa được khởi tạo (tức là null), nó sẽ tạo mới một AlertDialog và gán nền trong suốt cho hộp thoại.
        if(dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            // Dùng để inflate layout của hộp thoại xóa ghi chú từ tệp layout layout_delete_note.xml.
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
            );
            // builder.setView(view): Thiết lập layout vừa inflate vào hộp thoại.
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if(dialogDeleteNote.getWindow()!= null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            //  Đặt onClickListener cho nút xác nhận xóa ghi chú.
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //  Khi nút này được nhấn, một AsyncTask mới được tạo để xóa ghi chú.
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                        // //  Trong doInBackground của AsyncTask, ghi chú được xóa bằng cách gọi phương thức deleteNote
                        // từ NoteDao(một phần của Room Database)
                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao()
                                    .deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        // //  Sau khi xóa thành công, onPostExecute được gọi, đặt kết quả là ghi chú đã được xóa
                        // và kết thúc hoạt động hiện tại.
                        @Override
                        protected void onPostExecute(Void avoid) {
                            super.onPostExecute(avoid);
                            // Toast.makeText(CreateNoteActivity.this, "Note deleted successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDeleted", true);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                }
            });

            // view.findViewById(R.id.textCancel).setOnClickListener(...):
            // Đặt onClickListener cho nút hủy bỏ trong hộp thoại.
            // Khi nút này được nhấn, hộp thoại sẽ được đóng mà không xóa ghi chú.
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogDeleteNote.dismiss();
                }
            });
        }

        // dialogDeleteNote.show(): Cuối cùng, nếu hộp thoại chưa được hiển thị,
        // nó sẽ được hiển thị ra màn hình để người dùng xác nhận việc xóa ghi chú.
        dialogDeleteNote.show();
    }

    // Đoạn mã này thực hiện việc thiết lập màu sắc cho thanh chỉ mục phụ tiêu đề của ghi chú.
    private void setSubtitleIndicatorColor() {
        // Lấy Drawable của thành phần giao diện người dùng (View) viewSubtitleIndicator.
        // Trong trường hợp này, viewSubtitleIndicator có thể là một hình chữ nhật hoặc
        // một View khác với một hình dạng có thể được chỉnh sửa màu sắc.
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        // Điều này sẽ đặt màu sắc của Drawable đó thành màu được chọn thông qua biến selectedNoteColor.
        // Phương thức parseColor() của lớp Color được sử dụng để chuyển đổi một chuỗi màu hex (ví dụ: "#FDBE3B")
        // thành giá trị màu tương ứng. Sau đó, phương thức setColor() được gọi để đặt màu cho Drawable này.
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    // Đoạn mã này là một phần của chức năng chọn hình ảnh từ bộ nhớ hoặc thư viện của thiết bị.
    private void selectImage() {
        // Tạo một Intent với hành động ACTION_PICK, cho biết rằng chúng ta muốn chọn một phần tử từ dữ liệu được trả về
        // bởi một hoạt động được chỉ định.
        // MediaStore.Images.Media.EXTERNAL_CONTENT_URI là URI đại diện cho các hình ảnh được lưu trữ trên bộ nhớ ngoại của thiết bị.
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //  Phần này kiểm tra xem liệu thiết bị có hoạt động nào có thể xử lý Intent đã tạo hay không.
        //  Nếu không tìm thấy, ngoại lệ ActivityNotFoundException sẽ được ném và hiển thị một thông báo lỗi thông qua Toast
        //  cho người dùng biết rằng không có ứng dụng nào có thể chọn hình ảnh.
        try {
            // Bắt đầu một hoạt động mới để chọn hình ảnh từ nguồn dữ liệu được chỉ định bởi Intent.
            // Khi người dùng chọn một hình ảnh, kết quả sẽ được trả về thông qua onActivityResult() với mã yêu cầu REQUEST_CODE_SELECT_IMAGE.
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
        }
    }

    // Đoạn mã này xử lý kết quả từ việc yêu cầu cấp quyền từ người dùng. Khi bạn yêu cầu cấp quyền từ người dùng
    // (trong trường hợp này là quyền truy cập vào bộ nhớ), hệ thống sẽ hiển thị một hộp thoại cho phép hoặc từ chối quyền.
    @Override
    // Phương thức này được gọi khi người dùng đã đưa ra câu trả lời về việc cấp quyền hoặc từ chối quyền được yêu cầu.
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //  kiểm tra xem yêu cầu quyền có phải là yêu cầu truy cập bộ nhớ không và kết quả từ người dùng có dữ liệu không
        //  (tức là có kết quả từ người dùng hay không).
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0);{
            // Điều kiện này kiểm tra xem người dùng có đã cấp quyền hay không.
            // Nếu grantResults[0] bằng PackageManager.PERMISSION_GRANTED thì người dùng đã cấp quyền.
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Nếu người dùng đã cấp quyền, phương thức selectImage() được gọi để chọn hình ảnh.
                selectImage();
            } else {
                // Nếu không, một thông báo Toast sẽ được hiển thị thông báo rằng "Permission Denied",
                // cho biết người dùng đã từ chối quyền truy cập vào bộ nhớ.
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Đoạn mã này xử lý kết quả sau khi người dùng chọn một hình ảnh từ bộ nhớ của thiết bị thông qua một hoạt động đã được gọi trước đó.
    @Override
    // Phương thức này được gọi khi một hoạt động con (activity) đã kết thúc và trả về kết quả.
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // mã kiểm tra xem kết quả trả về có phải là từ hoạt động chọn hình ảnh không và có thành công không.
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            // Kiểm tra xem dữ liệu trả về có tồn tại hay không.
            if(data != null) {
                //  Lấy đường dẫn (URI) của hình ảnh được chọn từ dữ liệu trả về.
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null) {
                    try {
                        // Mở đầu vào dữ liệu từ URI của hình ảnh.
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        // Tạo một đối tượng Bitmap từ dữ liệu đầu vào. Đối tượng này chứa hình ảnh đã chọn.
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        //  Đặt hình ảnh đã chọn vào ImageView imageNote để hiển thị nó trên giao diện người dùng.
                        imageNote.setImageBitmap(bitmap);
                        // Hiển thị ImageView imageNote.
                        imageNote.setVisibility(View.VISIBLE);
                        //  Hiển thị một nút hoặc biểu tượng để xóa hình ảnh đã chọn.
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        // Lấy đường dẫn tuyệt đối của hình ảnh được chọn từ URI và lưu vào biến seletedImagePath.
                        // Đây có thể là đường dẫn tuyệt đối của hình ảnh trong bộ nhớ của thiết bị.
                        seletedImagePath = getPathFromUri(selectedImageUri);

                        // Trong trường hợp xảy ra ngoại lệ trong quá trình xử lý, một thông báo Toast sẽ hiển thị thông báo lỗi.
                    }catch (Exception exception) {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // Đoạn code này nhằm lấy đường dẫn thực của một tệp từ Uri của nó.
    // getPathFromUri(Uri contentUri): Phương thức nhận một đối tượng Uri làm đối số và trả về đường dẫn đến tệp tương ứng.
    // Đầu vào là contentUri - Uri của tệp tin cần lấy đường dẫn.
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        // getContentResolver().query(contentUri, null, null, null, null): Sử dụng ContentResolver để truy vấn thông tin của Uri,
        // thu được từ dịch vụ cung cấp dữ liệu cho ứng dụng Android. Truy vấn này trả về một con trỏ Cursor chứa thông tin về tệp cần tìm.
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        // ursor == null: Nếu không tìm thấy thông tin từ Uri, Cursor sẽ trở thành null.
        // Trong trường hợp này, contentUri.getPath() sẽ được sử dụng để lấy đường dẫn của Uri.
        if(cursor == null){
            filePath = contentUri.getPath();
        }else{
            // Nếu Cursor chứa dữ liệu, cursor.moveToFirst() di chuyển con trỏ đến hàng đầu tiên trong kết quả trả về.
            cursor.moveToFirst();
            // int index = cursor.getColumnIndex("_data"): Lấy chỉ số của cột "_data" từ Cursor, đây là cột thông thường chứa đường dẫn của tệp.
            int index = cursor.getColumnIndex("_data");
            // filePath = cursor.getString(index): Lấy đường dẫn thực sự của tệp từ cột "_data" thông qua Cursor.
            filePath = cursor.getString(index);
            // cursor.close(): Đóng Cursor sau khi đã sử dụng xong.
            cursor.close();
        }
        // Phương thức trả về filePath, tức là đường dẫn tệp thực tế từ Uri truyền vào, có thể được sử dụng cho các mục đích xử lý tệp tiếp theo.
        return filePath;
    }

    // Đoạn code này tạo và hiển thị một hộp thoại (dialog) để người dùng có thể thêm một URL cho ghi chú.
    private void showAddURLDialog() {
        // AlertDialog.Builder: Được sử dụng để xây dựng hộp thoại.
        if(dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            // LayoutInflater.from().inflate(): Nạp layout từ tệp tin layout_add_url.xml để sử dụng trong hộp thoại.
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            // builder.setView(view): Thiết lập layout đã nạp vào hộp thoại.
            builder.setView(view);

            // dialogAddURL = builder.create(): Tạo đối tượng dialog từ builder đã cấu hình trước đó.
            dialogAddURL = builder.create();
            if(dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            // Nếu người dùng nhấn "Thêm" (textAdd),
            // nó kiểm tra xem URL đã nhập có rỗng hay không và xem nó có phù hợp với định dạng URL hợp lệ
            // hay không thông qua Patterns.WEB_URL.matcher().
            // Nếu URL không hợp lệ, nó hiển thị một thông báo cảnh báo.
            // Nếu URL hợp lệ, nó cập nhật nội dung TextView textWebURL
            // và làm cho layout layoutWebURL trở thành VISIBLE, sau đó đóng hộp thoại.
            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(inputURL.getText().toString().trim().isEmpty()){
                        Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(CreateNoteActivity.this,"Enter valid URL", Toast.LENGTH_SHORT).show();
                    } else {
                        textWebURL.setText(inputURL.getText().toString());
                        layoutWebURL.setVisibility(View.VISIBLE);
                        dialogAddURL.dismiss();
                    }
                }
            });

            // Nếu dialogAddURL chưa được khởi tạo trước đó, nó sẽ tạo mới. Sau đó, nó sẽ hiển thị hộp thoại.
            // Nếu dialogAddURL đã tồn tại, nó sẽ chỉ hiển thị hộp thoại đã tạo trước đó. Điều này giúp tránh tạo mới dialog mỗi khi cần hiển thị.
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }

    private void setBold() {
        int start = inputNoteText.getSelectionStart();
        int end = inputNoteText.getSelectionEnd();
        if (start >= 0 && end >= 0) {
            if (isBold) {
                inputNoteText.getEditableText().setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                StyleSpan[] boldSpans = inputNoteText.getEditableText().getSpans(start, end, StyleSpan.class);
                for (StyleSpan span : boldSpans) {
                    if (span.getStyle() == Typeface.BOLD) {
                        inputNoteText.getEditableText().removeSpan(span);
                    }
                }
            }
        }
    }

    private void setItalic() {
        int start = inputNoteText.getSelectionStart();
        int end = inputNoteText.getSelectionEnd();
        if (start >= 0 && end >= 0) {
            if (isItalic) {
                inputNoteText.getEditableText().setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                StyleSpan[] italicSpans = inputNoteText.getEditableText().getSpans(start, end, StyleSpan.class);
                for (StyleSpan span : italicSpans) {
                    if (span.getStyle() == Typeface.ITALIC) {
                        inputNoteText.getEditableText().removeSpan(span);
                    }
                }
            }
        }
    }

    private void setUnderline() {
        int start = inputNoteText.getSelectionStart();
        int end = inputNoteText.getSelectionEnd();

        if (start >= 0 && end >= 0) {
            UnderlineSpan[] underlineSpans = inputNoteText.getEditableText().getSpans(start, end, UnderlineSpan.class);

            if (isUnderline) {
                // Apply underline to the selected text
                inputNoteText.getEditableText().setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // Remove underline from the selected text
                for (UnderlineSpan span : underlineSpans) {
                    inputNoteText.getEditableText().removeSpan(span);
                }
            }
        }
    }

    private void setTextAlignment() {
        inputNoteText.setTextAlignment(textAlignment);
        Spannable spannableString = new SpannableStringBuilder(inputNoteText.getText());
        inputNoteText.setText(spannableString);
    }
}