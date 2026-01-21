package com.google.easyapp.widget;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.easyapp.R;

public class DialogInputKey extends BaseDialog {
    private EditText editText;
    private Button okBtn;
    private OnClickOkListener listener;

    public DialogInputKey(Context context) {
        super(context);
    }

    @Override
    protected void bindView(View view) {
        this.editText = (EditText) view.findViewById(R.id.edit_tv);
        this.okBtn = (Button) view.findViewById(R.id.ok_btn);
        this.okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(editText.getText().toString());
                }
            }
        });
    }

    @Override
    protected int getLayout() {
        return R.layout.dialog_inputkey;
    }

    public String getEdit() {
        return this.editText.getText().toString();
    }

    public DialogInputKey setOkListenter(final OnClickOkListener listener) {
        this.listener = listener;
        return this;
    }

    public void show(String key) {
        editText.setText(key);
        super.show();
    }

    public interface OnClickOkListener {
        void onClick(String text);
    }

}
