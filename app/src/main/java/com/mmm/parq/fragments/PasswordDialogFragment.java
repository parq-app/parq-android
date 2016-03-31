package com.mmm.parq.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasPassword;

public class PasswordDialogFragment extends DialogFragment {
    private EditText mPasswordView;
    private PasswordSetListener mCallback;

    public interface PasswordSetListener extends HasPassword {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_password, null);
        mPasswordView = (EditText) view.findViewById(R.id.password);

        builder.setMessage("Enter your password to change your email:")
               .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       mCallback.setPassword(mPasswordView.getText().toString());
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       dismiss();
                   }
               })
               .setView(view);

        return builder.create();
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (PasswordSetListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }
}
