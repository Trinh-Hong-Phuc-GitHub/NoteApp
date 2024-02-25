package com.example.advanceandroidnotesapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.advanceandroidnotesapp.R;
import com.example.advanceandroidnotesapp.adapters.NotesAdapter;
import com.example.advanceandroidnotesapp.database.NotesDatabase;
import com.example.advanceandroidnotesapp.entities.Note;
import com.example.advanceandroidnotesapp.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private  int noteClickedPosition = -1;
    private AlertDialog dialogAddURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        notesRecyclerView = findViewById(R.id.noteRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // noteList.size() != 0: Điều kiện này kiểm tra xem danh sách ghi chú có chứa các mục không.
                // Nếu có ít nhất một mục trong danh sách, mã bên trong điều kiện sẽ được thực thi.
                if(noteList.size() != 0) {
                    //  Đây là cách để tìm kiếm và hiển thị các ghi chú dựa trên văn bản đã nhập vào trường tìm kiếm.
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        // Đoạn code này xử lý sự kiện khi người dùng nhấn vào một hình ảnh hoặc nút để thêm ghi chú mới.
        // Khi người dùng nhấn vào hình ảnh hoặc nút này, một intent mới được tạo để chuyển từ màn hình hiện tại
        // sang màn hình tạo ghi chú (CreateNoteActivity).
        // Khi màn hình tạo ghi chú hoàn thành công việc của nó và trở về màn hình hiện tại, kết quả được trả về thông qua startActivityForResult.
        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        // Đoạn code này xử lý sự kiện khi người dùng nhấn vào một hình ảnh
        // hoặc nút để chọn một hình ảnh từ bộ nhớ thiết bị.
        // Khi người dùng nhấn vào hình ảnh hoặc nút này, một sự kiện onClick được kích hoạt.
        findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đoạn code kiểm tra xem ứng dụng có quyền truy cập vào bộ nhớ thiết bị để đọc dữ liệu không.
                // Nếu không có quyền, ứng dụng sẽ yêu cầu cấp quyền sử dụng bộ nhớ bằng cách sử dụng ActivityCompat.requestPermissions.
                // Nếu đã có quyền truy cập, hàm selectImage() sẽ được gọi để chọn hình ảnh từ bộ nhớ thiết bị.
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                }else {
                    selectImage();
                }
            }
        });

        findViewById(R.id.imageAddWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddURLDialog();
            }
        });
    }

    // Đoạn code trên được gọi trong phương thức onResume(), một trong các phương thức vòng đời của Activity.
    // Khi Activity này trở lại trạng thái foreground (trạng thái mà người dùng có thể tương tác được), onResume() được gọi.
    //add
    @Override
    protected void onResume(){
        super.onResume();
        // notesRecyclerView.setAdapter(notesAdapter);: Đây là lệnh để thiết lập Adapter cho RecyclerView notesRecyclerView.
        // Adapter được sử dụng để cung cấp dữ liệu cho RecyclerView để hiển thị trên giao diện người dùng.
        notesRecyclerView.setAdapter(notesAdapter);
        // getNotes(REQUEST_CODE_SHOW_NOTES, false);: Đây là một phương thức để lấy danh sách ghi chú từ cơ sở dữ liệu.
        // REQUEST_CODE_SHOW_NOTES được sử dụng để xác định rằng việc gọi getNotes đang được thực hiện để hiển thị danh sách các ghi chú.
        // Tham số thứ hai là false, ngụ ý là không có ghi chú nào bị xóa trong quá trình này.
        getNotes(REQUEST_CODE_SHOW_NOTES, false);
    }
    //end

    // Trong đoạn code trên, onRequestPermissionsResult là một phương thức được gọi khi người dùng đã cung cấp hoặc
    // từ chối cấp quyền mà ứng dụng yêu cầu. Hàm này được gọi khi người dùng phản hồi vào hộp thoại yêu cầu cấp quyền từ ứng dụng.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            // Nếu requestCode là REQUEST_CODE_STORAGE_PERMISSION và có kết quả được trả về từ việc cấp quyền (grantResults.length > 0),
            // điều kiện trong if sẽ kiểm tra xem quyền đã được cấp hay không.
            // Nếu quyền đã được cấp (grantResults[0] == PackageManager.PERMISSION_GRANTED),
            // hàm selectImage() sẽ được gọi để chọn hình ảnh hoặc tệp tin từ bộ nhớ của thiết bị.
            //Trong trường hợp ngược lại, một thông báo Toast sẽ xuất hiện thông báo rằng "Permission Denied", cho biết quyền đã bị từ chối.
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Đoạn code này có nhiệm vụ nhận một Uri đại diện cho một tài nguyên
    // (ví dụ: một tệp được chọn từ bộ nhớ thiết bị) và trả về đường dẫn tệp tương ứng với Uri đó.
    // Đầu vào của hàm là contentUri, đại diện cho Uri của tài nguyên.
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        // Trước tiên, nó tạo một đối tượng Cursor bằng cách truy vấn ContentResolver với contentUri.
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        // Nếu cursor không rỗng (không null), nó di chuyển con trỏ của cursor đến hàng đầu tiên
        // và sau đó lấy đường dẫn tệp thông qua chỉ mục _data.
        // Nếu cursor là null hoặc không chứa dữ liệu, filePath được thiết lập bằng đường dẫn từ contentUri
        // trực tiếp thông qua contentUri.getPath().
        if(cursor == null){
            filePath = contentUri.getPath();
        }else{
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        // Hàm trả về filePath, tức là đường dẫn của nội dung từ contentUri.
        return filePath;
    }

    // Đoạn code này tạo một Intent để chọn một hình ảnh từ bộ nhớ ngoại vi của thiết bị.
    //Intent được khởi tạo với hành động Intent.ACTION_PICK để chọn một hình ảnh.
    //MediaStore.Images.Media.EXTERNAL_CONTENT_URI được sử dụng để chỉ định rằng chúng ta muốn lấy hình ảnh từ bộ nhớ ngoại vi của thiết bị.
    // Sau đó, startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE) được gọi để mở một hoạt động để chọn hình ảnh từ bộ nhớ và chờ kết quả trả về.
    // Nếu không có ứng dụng nào có thể xử lý hành động này trên thiết bị, ActivityNotFoundException sẽ được ném ra và một thông báo lỗi sẽ được hiển thị thông qua Toast.
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //   if(intent.resolveActivity(getPackageManager())!= null) {
        //     startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        try {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
        }
    }


    // Đoạn code này được gọi khi người dùng nhấn vào một ghi chú trong danh sách ghi chú.
    // Cụ thể, đây là một phương thức xử lý sự kiện khi một ghi chú được nhấn trong danh sách.
    @Override
    public void onNoteClicked(Note note, int position) {
        // noteClickedPosition = position;: Lưu trữ vị trí của ghi chú được nhấn trong danh sách.
        noteClickedPosition = position;
        // Tạo một Intent để mở hoạt động CreateNoteActivity (intent.putExtra("isViewOrUpdate", true);).
        // Đặt các dữ liệu bổ sung trong Intent:
        // isViewOrUpdate là một boolean để cho hoạt động biết rằng nó cần hiển thị hoặc cập nhật ghi chú.
        // note là đối tượng ghi chú cụ thể mà người dùng đã nhấn.
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        // Cuối cùng, sử dụng startActivityForResult() để mở CreateNoteActivity và chờ kết quả trả về
        // (sử dụng REQUEST_CODE_UPDATE_NOTE để nhận diện kết quả).
        // Điều này cho phép hoạt động ghi chú được trả về và xử lý dữ liệu khi hoạt động CreateNoteActivity kết thúc.
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }

    // Đoạn code này chứa một phương thức được sử dụng để lấy dữ liệu ghi chú từ cơ sở dữ liệu
    // và cập nhật giao diện người dùng dựa trên kết quả trả về.
    // getNotes() là phương thức được sử dụng để truy vấn cơ sở dữ liệu và cập nhật giao diện người dùng tương ứng với yêu cầu cụ thể.
    private void getNotes(final int requestCode, final boolean isNoteDeleted) {

        // Tạo một AsyncTask (GetNotesTask) để thực hiện truy vấn cơ sở dữ liệu trên một luồng riêng biệt.
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            // doInBackground(): Thực hiện truy vấn cơ sở dữ liệu để lấy danh sách các ghi chú.
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            // onPostExecute(): Xử lý kết quả trả về từ truy vấn cơ sở dữ liệu.
            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if(requestCode  == REQUEST_CODE_SHOW_NOTES){
                    // Nếu requestCode là REQUEST_CODE_SHOW_NOTES:
                    // Xóa tất cả các ghi chú khỏi danh sách noteList.
                    // Thêm tất cả các ghi chú mới từ notes vào noteList.
                    // Cập nhật dữ liệu trong notesAdapter thông qua notifyDataSetChanged() để hiển thị dữ liệu mới trên giao diện người dùng.
                    noteList.clear();
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }else if(requestCode == REQUEST_CODE_ADD_NOTE){
                    // Nếu requestCode là REQUEST_CODE_ADD_NOTE:
                    // Thêm ghi chú mới vào vị trí đầu tiên của noteList và cập nhật giao diện người dùng để thể hiện ghi chú mới.
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    // Nếu requestCode là REQUEST_CODE_UPDATE_NOTE:
                    // Xóa ghi chú tại vị trí noteClickedPosition khỏi danh sách noteList.
                    // Nếu ghi chú bị xóa (isNoteDeleted là true), thông báo cho notesAdapter biết để xóa ghi chú khỏi giao diện.
                    // Nếu ghi chú được cập nhật, thêm ghi chú đã cập nhật vào noteList tại vị trí noteClickedPosition
                    // và cập nhật giao diện người dùng thông qua notesAdapter.
                    noteList.remove(noteClickedPosition);
                    if(isNoteDeleted){
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    }else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }
        // được sử dụng để khởi chạy một AsyncTask mới để thực hiện công việc trên một luồng nền riêng biệt.
        new GetNotesTask().execute();
    }

    // Đoạn code này xử lý kết quả trả về từ các hoạt động khác được gọi bằng phương thức startActivityForResult().
    // Hành động được thực hiện dựa trên mã yêu cầu (requestCode) và kết quả (resultCode) trả về từ các hoạt động đã được gọi trước đó.
    @Override
    // onActivityResult là phương thức được gọi khi một hoạt động con kết thúc và trả về kết quả cho hoạt động gốc.
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Thêm ghi chú mới (REQUEST_CODE_ADD_NOTE):
        // Nếu hoạt động trước đó là để thêm ghi chú và trả về kết quả OK, hàm getNotes được gọi
        // để cập nhật danh sách ghi chú. Mã yêu cầu được truyền vào để phân biệt các hoạt động.
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        }
        // Nếu hoạt động trước đó là cập nhật ghi chú và trả về kết quả OK, hàm getNotes được gọi để cập nhật danh sách ghi chú.
        // Kiểm tra xem có dữ liệu được trả về không và nếu có, lấy giá trị boolean "isNoteDeleted" từ dữ liệu kết quả.
        // Giá trị này xác định liệu ghi chú đã bị xóa hay không và dùng để cập nhật danh sách ghi chú.
        else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
        // else if(requestCode == REQUEST_CODE_UPDATE_NOTE){
            if(data!=null){
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted",false));
            }
            // Nếu hoạt động trước đó là chọn hình ảnh và trả về kết quả OK, phương thức này sẽ lấy
            // đường dẫn của hình ảnh từ Uri và tạo một Intent để chuyển đến CreateNoteActivity.
            //Intent này chứa thông tin về hình ảnh đã chọn dưới dạng Intent Extra và g
            // ọi hoạt động CreateNoteActivity để thêm ghi chú mới với hình ảnh đã chọn.
        }else if(requestCode==REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data != null) {
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickAction",true);
                        intent.putExtra("quickActionType","image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }catch(Exception exception){
                        Toast.makeText(this, exception.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // Đoạn code này tạo và hiển thị một hộp thoại khi người dùng muốn thêm một URL mới vào ghi chú.
    // Hộp thoại này cho phép người dùng nhập URL và sau đó thêm URL đó vào ghi chú.
    // showAddURLDialog() tạo một hộp thoại và hiển thị nó.
    private void showAddURLDialog() {
        // Trước tiên, kiểm tra xem biến dialogAddURL có rỗng không. Nếu rỗng, tạo một AlertDialog mới và gán nó vào biến dialogAddURL.
        if(dialogAddURL == null) {
            // Lấy layout XML được định nghĩa trong R.layout.layout_add_url và gắn nó vào AlertDialog được tạo.
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddURL = builder.create();
            // Nếu cửa sổ của AlertDialog không null, thiết lập nền của cửa sổ đó thành một ColorDrawable để làm mờ nền xung quanh.
            if(dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            // Thiết lập các sự kiện cho các nút trong hộp thoại:
            // Khi người dùng nhấn nút "Add", kiểm tra xem trường nhập URL có trống không. Nếu trống, hiển thị một thông báo cảnh báo người dùng nhập URL.
            // Nếu URL không hợp lệ (không khớp với mẫu URL), hiển thị thông báo yêu cầu nhập một URL hợp lệ.
            // Nếu URL hợp lệ, tạo một Intent để chuyển đến CreateNoteActivity và gửi thông tin về URL đã nhập dưới dạng Intent Extra.
            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(inputURL.getText().toString().trim().isEmpty()){
                        Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(MainActivity.this,"Enter valid URL", Toast.LENGTH_SHORT).show();
                    } else {
                        dialogAddURL.dismiss();
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickAction",true);
                        intent.putExtra("quickActionType","URL");
                        intent.putExtra("URL", inputURL.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            // Khi người dùng nhấn nút "Cancel", đơn giản chỉ đóng hộp thoại.
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddURL.dismiss();
                }
            });
        }
        // Cuối cùng, sau khi hộp thoại được xử lý, nó được hiển thị bằng cách gọi dialogAddURL.show().
        dialogAddURL.show();
    }
}