package com.example.advanceandroidnotesapp.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.advanceandroidnotesapp.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

    //  Đây là một annotation của Room Persistence Library trong Android,
    //  cho phép bạn thực hiện một truy vấn SQL trên cơ sở dữ liệu SQLite.
    //  Trong trường hợp này, nó định nghĩa một truy vấn để lấy tất cả các ghi chú từ bảng notes
    //  và sắp xếp chúng theo trường id theo thứ tự giảm dần.
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();

    //  Annotation này cũng là một phần của Room.
    //  Nó đánh dấu một phương thức để chèn một ghi chú mới vào cơ sở dữ liệu.
    //  Nếu đã tồn tại một ghi chú có cùng khóa chính, thì hành vi "on conflict" sẽ được áp dụng.
    //  Trong trường hợp này, OnConflictStrategy.REPLACE chỉ định rằng nếu có xung đột (trùng khóa chính),
    //  ghi chú cũ sẽ được thay thế bởi ghi chú mới.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    // Đây là một annotation khác, đánh dấu một phương thức để xóa một ghi chú khỏi cơ sở dữ liệu.
    // Nó sẽ xóa ghi chú có trong cơ sở dữ liệu mà khớp với đối tượng Note được truyền vào phương thức deleteNote.
    @Delete
    void deleteNote(Note note);

}
