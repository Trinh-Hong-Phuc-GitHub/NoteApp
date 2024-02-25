package com.example.advanceandroidnotesapp.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.advanceandroidnotesapp.R;
import com.example.advanceandroidnotesapp.entities.Note;
import com.example.advanceandroidnotesapp.listeners.NotesListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    // Đây là danh sách các đối tượng ghi chú (Note) được chuyển vào adapter từ bên ngoài.
    private List<Note> notes;
    // Một interface NotesListener được sử dụng để lắng nghe các sự kiện khi một ghi chú được click hoặc thao tác khác được thực hiện.
    private NotesListener notesListener;
    private Timer timer;
    private List<Note> notesSource;

    // Đoạn mã này là constructor của NotesAdapter, được sử dụng để khởi tạo một instance của adapter. Có một số thứ cần lưu ý ở đây:
    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        // Biến này lưu trữ danh sách ghi chú gốc. Ban đầu, nó được gán giá trị bằng notes, tức là danh sách ghi chú được chuyển vào từ bên ngoài.
        notesSource = notes;
    }

    //Đoạn mã này định nghĩa cách tạo một NoteViewHolder mới khi được yêu cầu bởi RecyclerView.
    // Mỗi khi RecyclerView cần một ViewHolder mới để hiển thị một item trong danh sách, phương thức này được gọi.
    @NonNull
    @Override
    //  tạo một NoteViewHolder mới bằng cách sử dụng LayoutInflater để inflate layout từ R.layout.item_container_note.
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_note,
                        //  Là ViewGroup mà ViewHolder mới sẽ được gắn vào sau khi tạo.
                        parent,
                        false
                        // Đây là giá trị cho tham số attachToRoot trong inflate(),
                        // chỉ định rằng ViewHolder được tạo ra không nên được gắn vào ViewGroup parent ngay lập tức.
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        holder.setNote(notes.get(position));
        // add
        //holder.imageNote.setVisibility(View.GONE);
        //end
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notesListener.onNoteClicked(notes.get(position), position);
               // notesListener.onNoteClicked(notes.get(holder.getAdapterPosition()), holder.getAdapterPosition());
            }
        });
    }

    // Phương thức này được gọi để trả về số lượng các item trong RecyclerView.
    // Trong trường hợp này, nó trả về kích thước của danh sách notes, nơi chứa các dữ liệu ghi chú cần hiển thị.
    @Override
    public int getItemCount() {
        return notes.size();
    }

    // Phương thức này được sử dụng để trả về kiểu của item tại vị trí position trong RecyclerView.
    // trường hợp này, nó trả về giá trị position (vị trí) của item, thường được sử dụng khi có nhiều loại item trong RecyclerView.
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // Đây là một lớp ViewHolder tĩnh (static) bên trong NotesAdapter.
    // Nó mô tả cách các thành phần giao diện của một item trong RecyclerView sẽ được ánh xạ và quản lý thông qua ViewHolder.
    static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textTitle, textSubtitle, textDateTime;
        LinearLayout layoutNote;
        RoundedImageView imageNote;
        //  Đây là constructor của lớp NoteViewHolder, nhận vào một View là itemView, đại diện cho layout của một item trong RecyclerView.
        // Điều này cho phép NoteViewHolder sử dụng các thành phần giao diện đã được tìm thấy trong itemView để hiển thị dữ liệu cho mỗi item trong RecyclerView.
        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            imageNote = itemView.findViewById(R.id.imageNote);
        }

        // Đoạn mã setNote() trong lớp NoteViewHolder của NotesAdapter
        // được sử dụng để thiết lập dữ liệu cho một ghi chú cụ thể hiển thị trong RecyclerView.
        void setNote(Note note) {
            textTitle.setText(note.getTitle());
            if(note.getSubtitle().trim().isEmpty()) {
                textSubtitle.setVisibility(View.GONE);
            } else{
                textSubtitle.setText(note.getSubtitle());
            }
            textDateTime.setText(note.getDatetime());

            //  Điều chỉnh màu nền của layoutNote (có thể là một LinearLayout hoặc một thành phần giao diện khác)
            //  dựa trên màu được cung cấp trong Note.
            //  Nếu note.getColor() trả về null, màu mặc định sẽ là #333333.
            //  Nếu không, màu nền sẽ được thiết lập dựa trên màu được cung cấp từ Note.
            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            if(note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            // Nếu đường dẫn đến hình ảnh được cung cấp trong Note (qua getImagePath()),
            // hình ảnh này sẽ được giải mã và hiển thị trong imageNote (có thể là một RoundedImageView hoặc một thành phần giao diện khác).
            // Nếu không có hình ảnh (note.getImagePath() == null), imageNote sẽ được ẩn đi (setVisibility(View.GONE)).
            if(note.getImagePath() != null) {
                imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                imageNote.setVisibility(View.VISIBLE);
            } else {
                imageNote.setVisibility(View.GONE);
            }
        }
    }

    // Đoạn mã này định nghĩa một phương thức searchNotes trong NotesAdapter.
    // Phương thức này thực hiện tìm kiếm các ghi chú dựa trên các từ khóa tìm kiếm được nhập vào.
    public void searchNotes(final String searchkeywords){
        timer = new Timer(); // Tạo một Timer và lên lịch cho một TimerTask.
                timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Trong TimerTask, trước hết, nó kiểm tra xem từ khóa tìm kiếm có rỗng không (searchkeywords.trim().isEmpty()).
                // Nếu rỗng, notes sẽ được gán bằng notesSource, tức là hiển thị toàn bộ danh sách ghi chú ban đầu.
                if(searchkeywords.trim().isEmpty()){
                    notes = notesSource;
                }else {
                    // Nếu không rỗng, nó tạo một danh sách tạm thời temp và duyệt qua danh sách gốc notesSource.
                    // Mỗi ghi chú sẽ được kiểm tra xem có chứa từ khóa tìm kiếm trong tiêu đề, phụ đề hoặc nội dung ghi chú không.
                    // Nếu thỏa mãn điều kiện tìm kiếm, ghi chú sẽ được thêm vào danh sách tạm temp.
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : notesSource) {
                        if(note.getTitle().toLowerCase().contains(searchkeywords.toLowerCase())
                        || note.getSubtitle().toLowerCase().contains(searchkeywords.toLowerCase())
                        || note.getNoteText().toLowerCase().contains(searchkeywords.toLowerCase())){
                            // Sau khi tìm kiếm hoàn tất, danh sách notes sẽ được gán bằng temp, chứa các ghi chú thỏa mãn điều kiện tìm kiếm.
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
                // Cuối cùng, sau khi danh sách notes đã được cập nhật,
                // dòng mã notifyDataSetChanged() được gọi trong một Handler để đảm bảo rằng nó chạy trên luồng giao diện chính (UI thread).
                // Điều này là cần thiết để cập nhật giao diện người dùng sau khi danh sách ghi chú đã được tìm kiếm và cập nhật.
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }, 500); // TimerTask này sẽ chạy sau một khoảng thời gian nhất định (ở đây là 500 milliseconds) sau khi phương thức searchNotes được gọi.
    }

    // Phương thức cancelTimer() trong NotesAdapter có chức năng hủy bỏ bất kỳ Timer nào đang chạy. Hãy xem xét từng phần:
    public void cancelTimer(){
        // Điều kiện này kiểm tra xem có một đối tượng Timer được khởi tạo và gán cho biến timer hay không.
        if(timer != null){
            // Nếu timer không phải là null, tức là đã tồn tại một Timer, lệnh timer.cancel() được gọi để hủy bỏ việc chạy của Timer đó.
            //Hành động này dừng việc thực hiện TimerTask được gọi thông qua Timer.
            timer.cancel();
        }
        // Phương thức cancelTimer() này được sử dụng để ngừng hoạt động của Timer,
        // chẳng hạn như trong trường hợp ngừng tìm kiếm ghi chú theo từ khóa hoặc khi không cần thiết
        // để ngăn chặn việc cập nhật giao diện người dùng nếu người dùng vẫn đang tương tác với ứng dụng.
    }
}
