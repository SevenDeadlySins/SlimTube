package com.theeastwatch.slimtube;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/*
 * The dialog box for selecting video quality.
 */
public class QualityDialogFragment extends DialogFragment {
    static QualityDialogFragment newInstance(ArrayList<String> qualities, int selected) {
        QualityDialogFragment f = new QualityDialogFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("qualities", qualities);
        args.putInt("selected", selected);
        f.setArguments(args);

        return f;
    }

    public interface QualitySelectedListener {
        void onQualitySelected (int selected);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ArrayList<String> qualities = getArguments().getStringArrayList("qualities");
        Log.d("QualityDialogFragment", qualities.toString());
        int selected = getArguments().getInt("selected");
        Log.d("QualityDialogFragment", String.valueOf(selected));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_quality)
                .setSingleChoiceItems(qualities.toArray(new String[qualities.size()]), selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        QualitySelectedListener activity = (QualitySelectedListener) getActivity();
                        activity.onQualitySelected(which);
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}

