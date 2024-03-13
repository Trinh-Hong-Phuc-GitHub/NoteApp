package com.example.advanceandroidnotesapp.listeners;

import com.example.advanceandroidnotesapp.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
