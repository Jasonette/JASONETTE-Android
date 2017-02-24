package com.jasonette.seed.Action;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.media.CamcorderProfile.get;

public class JasonUtilAction {
    public void banner(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    Snackbar snackbar = Snackbar.make(((JasonViewActivity)context).rootLayout, options.getString("title") + "\n" + options.getString("description"), Snackbar.LENGTH_LONG);
                    snackbar.show();
                } catch (Exception e){
                    Log.d("Error", e.toString());
                }
            }
        });
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }
    public void toast(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, (CharSequence)options.getString("text"), duration);
                    toast.show();
                } catch (Exception e){
                    Log.d("Error", e.toString());
                }
            }
        });
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception err) {
            Log.d("Error", err.toString());
        }
    }
    public void alert(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                try {
                    JSONObject options = new JSONObject();
                    final ArrayList<EditText> textFields = new ArrayList<EditText>();
                    if (action.has("options")) {
                        options = action.getJSONObject("options");
                        String title = options.getString("title");
                        String description = options.getString("description");
                        builder.setTitle(title);
                        builder.setMessage(description);

                        if(options.has("form"))
                        {
                            LinearLayout lay = new LinearLayout(context);
                            lay.setOrientation(LinearLayout.VERTICAL);
                            lay.setPadding(20,5,20,5);
                            JSONArray formArr =  options.getJSONArray("form");
                            for (int i=0; i<formArr.length();i++) {
                                JSONObject obj = formArr.getJSONObject(i);
                                final EditText textBox = new EditText(context);
                                if(obj.has("placeholder")){
                                    textBox.setHint(obj.getString("placeholder"));
                                }
                                if(obj.has("type") && obj.getString("type").equals("secure")){
                                    textBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                }
                                if(obj.has("value")){
                                    textBox.setText(obj.getString("value"));
                                }
                                textBox.setTag(obj.get("name"));
                                lay.addView(textBox);
                                textFields.add(textBox);
                                builder.setView(lay);
                            }
                            // Set focous on first text field
                            final EditText focousedTextField = (EditText)textFields.get(0);
                            focousedTextField.post(new Runnable() {
                                public void run() {
                                    focousedTextField.requestFocus();
                                    InputMethodManager lManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    lManager.showSoftInput(focousedTextField, 0);
                                }
                            });


                        }


                    }
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    try {
                                        if (action.has("success")) {
                                            JSONObject postObject = new JSONObject();
                                            if(action.getJSONObject("options").has("form")){
                                                for(int i = 0; i<textFields.size();i++){

                                                    EditText textField = (EditText) textFields.get(i);
                                                    postObject.put(textField.getTag().toString(),textField.getText().toString());
                                                }
                                            }
                                            JasonHelper.next("success", action, postObject, event, context);
                                        }
                                    } catch (Exception err) {

                                    }
                                }
                            });
                    builder.setNeutralButton("CANCEL",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    //
                                }
                            });
                    builder.show();
                } catch (Exception e) {
                    Log.d("Error", e.toString());
                }
            }
        });
    }
}
