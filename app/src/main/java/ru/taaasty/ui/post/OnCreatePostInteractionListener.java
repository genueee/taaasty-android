package ru.taaasty.ui.post;


public interface OnCreatePostInteractionListener {

    void onValidationStatusChanged(boolean postValid);

    void onChoosePhotoButtonClicked(boolean hasPicture); // Отдельный интерфейс?

}
