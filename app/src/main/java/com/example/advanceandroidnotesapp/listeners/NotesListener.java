package com.example.advanceandroidnotesapp.listeners;

import com.example.advanceandroidnotesapp.entities.Note;

public interface NotesListener {
    // Phương thức onNoteClicked được định nghĩa trong interface NotesListener
    // có nhiệm vụ xử lý sự kiện khi một ghi chú được click. Đối số đầu tiên là Note note,
    // đó là đối tượng Note tương ứng với ghi chú được click.
    // Đối số thứ hai là int position, đó là vị trí của ghi chú trong danh sách hiển thị.
    void onNoteClicked(Note note, int position);
}
