package ru.taaasty.ui.post;


public interface OnCreatePostInteractionListener {

    public void onValidationStatusChanged(boolean postValid);

    public void onChoosePhotoButtonClicked(); // Отдельный интерфейс?
}
