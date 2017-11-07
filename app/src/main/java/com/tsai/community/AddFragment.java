package com.tsai.community;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddFragment extends Fragment {


    public AddFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add, container, false);
        EditText ed_AddContent = (EditText) v.findViewById(R.id.ed_AddContent);
        ed_AddContent.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        ed_AddContent.setSingleLine(false);
        ed_AddContent.setHorizontallyScrolling(false);
        return v;
    }

}
