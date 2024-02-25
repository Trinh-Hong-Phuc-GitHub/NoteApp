package com.example.advanceandroidnotesapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.advanceandroidnotesapp.dao.NoteDao;
import com.example.advanceandroidnotesapp.entities.Note;

// Đoạn code trên định nghĩa một lớp abstract NotesDatabase là một đối tượng cơ sở dữ liệu Room. Các điểm chính:
// Đánh dấu lớp này là một cơ sở dữ liệu Room.
// Annotation này xác định các entities (đối tượng) được sử dụng trong cơ sở dữ liệu,
// trong trường hợp này là Note.class. Phiên bản của cơ sở dữ liệu được đặt là 1 (version = 1).
// exportSchema = false được sử dụng để ngăn Room xuất ra file schema SQL.
@Database(entities = Note.class, version = 1, exportSchema = false)
// Một biến tĩnh được sử dụng để lưu trữ instance của cơ sở dữ liệu. Biến này sẽ được khởi tạo khi lớp NotesDatabase được sử dụng.
public abstract class NotesDatabase extends RoomDatabase {
    private static NotesDatabase notesDatabase;

    // Phương thức này trả về một instance của NotesDatabase để thực hiện các hoạt động trên cơ sở dữ liệu.
    // Nó sử dụng Room.databaseBuilder để tạo hoặc truy cập cơ sở dữ liệu, với các tham số bao gồm context,
    // lớp cơ sở dữ liệu (NotesDatabase.class), và tên của cơ sở dữ liệu ("notes_db").
    // Phương thức này là synchronized để đảm bảo chỉ có một luồng có thể truy cập cùng một lúc, tránh tình trạng xung đột dữ liệu.
    public static synchronized NotesDatabase getDatabase(Context context) {
        if(notesDatabase == null) {
            notesDatabase = Room.databaseBuilder(
                    context,
                    NotesDatabase.class,
                    "notes_db"
            ).build();
        }
        return notesDatabase;
    }
    // Đây là một phương thức abstract để trả về một đối tượng NoteDao.
    // Interface NoteDao được Room sử dụng để tương tác với cơ sở dữ liệu cho các thao tác như
    // truy vấn, chèn, xóa, và cập nhật dữ liệu trong bảng notes.

    public abstract NoteDao noteDao();
}
