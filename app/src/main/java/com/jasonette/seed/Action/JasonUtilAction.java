package com.jasonette.seed.Action;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Helper.JasonImageHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class JasonUtilAction {
    private int counter; // general purpose counter;
    private Intent callback_intent;  // general purpose intent;

    public void banner(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    String title = "Notice";
                    String result = "";
                    if (options.has("title")) {
                        title = options.get("title").toString();
                    }
                    if (options.has("description")) {
                        result = title + "\n" + options.get("description").toString();
                    } else {
                        result = title;
                    }
                    Snackbar snackbar = Snackbar.make(((JasonViewActivity)context).rootLayout, result, Snackbar.LENGTH_LONG);
                    snackbar.show();
                } catch (Exception e){
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        });
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void toast(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, (CharSequence)options.get("text").toString(), duration);
                    toast.show();
                } catch (Exception e){
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        });
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
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
                        String title = options.get("title").toString();
                        String description = options.get("description").toString();
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
                                    JasonHelper.next("error", action, new JSONObject(), event, context);
                                }
                            });
                    builder.show();
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        });
    }
    public void picker(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    if(options.has("items")){
                        final JSONArray items = options.getJSONArray("items");
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        if(options.has("title")){
                            String title = options.get("title").toString();
                            builder.setTitle(title);
                        }

                        ArrayList<String> listItems = new ArrayList<String>();
                        for (int i = 0; i < items.length() ; i++) {
                            JSONObject item = (JSONObject)items.get(i);
                            if(item.has("text")){
                               listItems.add(item.getString("text"));
                            } else {
                                listItems.add("");
                            }
                        }
                        final CharSequence[] charSequenceItems = listItems.toArray(new CharSequence[listItems.size()]);
                        builder.setItems(charSequenceItems, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int val) {
                                JSONObject item;
                                try {
                                    item = items.getJSONObject(val);
                                    if(item.has("action")){
                                        Intent intent = new Intent("call");
                                        intent.putExtra("action", item.get("action").toString());
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                    } else if(item.has("href")){
                                        Intent intent = new Intent("call");
                                        JSONObject href = new JSONObject();
                                        href.put("type", "$href");
                                        href.put("options", item.get("href"));
                                        intent.putExtra("action", href.toString());
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                    }
                                } catch (Exception e){
                                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                }
                            }
                        });
                        builder.setNeutralButton("CANCEL",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int val) {
                                }
                            }
                        );
                        builder.create().show();
                    }
                } catch (Exception e){
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        });
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
        }
    }


    public void datepicker(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new SingleDateAndTimePickerDialog.Builder(context)
                .bottomSheet()
                .curved()
                //.minutesStep(15)
                //.title("Simple")
                .listener(new SingleDateAndTimePickerDialog.Listener() {
                    @Override
                    public void onDateSelected(Date date) {
                        try {
                            String val = String.valueOf(date.getTime()/1000);
                            JSONObject value = new JSONObject();
                            value.put("value", val);
                            JasonHelper.next("success", action, value, event, context);
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }

                    }
                }).display();

    }


    public void addressbook(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getContacts(action, data, event, context);
            }
        }).start();
    }
    private void getContacts(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONArray contactList = new JSONArray();
        String phoneNumber = null;
        String email = null;
        ContentResolver contentResolver = ((JasonViewActivity)context).getContentResolver();
        try {
            final Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor.getCount() > 0) {
                counter = 0;
                while (cursor.moveToNext()) {
                    JSONObject contact = new JSONObject();
                    String contact_id = cursor.getString(cursor.getColumnIndex( ContactsContract.Contacts._ID ));
                    String name = cursor.getString(cursor.getColumnIndex( ContactsContract.Contacts.DISPLAY_NAME ));
                    try {
                        // name
                        contact.put("name", name);

                        // phone
                        Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{contact_id}, null);
                        while (phoneCursor.moveToNext()) {
                            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            if(phoneNumber != null) {
                                contact.put("phone", phoneNumber);
                            }
                        }
                        if(!contact.has("phone")){
                            contact.put("phone", "");
                        }
                        phoneCursor.close();

                        // email
                        Cursor emailCursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{contact_id}, null);
                        while (emailCursor.moveToNext()) {
                            email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            if(email != null) {
                                contact.put("email", email);
                            }
                        }
                        if(!contact.has("email")){
                            contact.put("email", "");
                        }
                        emailCursor.close();

                        // Add to array
                        contactList.put(contact);
                    } catch (Exception e){
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
                try {
                    JasonHelper.next("success", action, contactList, event, context);
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        } catch (SecurityException e){
            JasonHelper.permission_exception("$util.addressbook", context);
        }
    }

    public void share(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    if (options.has("items")) {
                        callback_intent = new Intent();
                        callback_intent.setAction(Intent.ACTION_SEND);

                        final JSONArray items = options.getJSONArray("items");
                        counter = 0;
                        final int l = items.length();
                        for (int i = 0; i < l; i++) {
                            JSONObject item = (JSONObject) items.get(i);
                            if (item.has("type")) {
                                String type = item.getString("type");
                                if (type.equalsIgnoreCase("text")) {
                                    callback_intent.putExtra(Intent.EXTRA_TEXT, item.get("text").toString());
                                    if (callback_intent.getType() == null) {
                                        callback_intent.setType("text/plain");
                                    }
                                    counter++;
                                    if (counter == l) {
                                        JasonHelper.next("success", action, new JSONObject(), event, context);
                                        context.startActivity(Intent.createChooser(callback_intent, "Share"));
                                    }
                                } else if (type.equalsIgnoreCase("image")) {
                                    // Fetch remote url
                                    // Turn it into Bitmap
                                    // Create a file
                                    // Share the url
                                    if (item.has("url")) {
                                        JasonImageHelper helper = new JasonImageHelper(item.getString("url"), context);
                                        helper.setListener(new JasonImageHelper.JasonImageDownloadListener() {
                                            @Override
                                            public void onLoaded(byte[] data, Uri uri) {
                                                callback_intent.putExtra(Intent.EXTRA_STREAM, uri);
                                                // override with image type if one of the items is an image
                                                callback_intent.setType("image/*");
                                                callback_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                counter++;
                                                if (counter == l) {
                                                    JasonHelper.next("success", action, new JSONObject(), event, context);
                                                    context.startActivity(Intent.createChooser(callback_intent, "Share"));
                                                }
                                            }
                                        });
                                        helper.fetch();
                                    } else if (item.has("data")) {
                                        // "data" is a byte[] stored as string
                                        // so we need to restore it back to byte[] before working with it.
                                        byte[] d = Base64.decode(item.getString("data"), Base64.DEFAULT);

                                        JasonImageHelper helper = new JasonImageHelper(d, context);
                                        helper.setListener(new JasonImageHelper.JasonImageDownloadListener() {
                                            @Override
                                            public void onLoaded(byte[] data, Uri uri) {
                                                callback_intent.putExtra(Intent.EXTRA_STREAM, uri);
                                                // override with image type if one of the items is an image
                                                callback_intent.setType("image/*");
                                                callback_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                counter++;
                                                if (counter == l) {
                                                    JasonHelper.next("success", action, new JSONObject(), event, context);
                                                    context.startActivity(Intent.createChooser(callback_intent, "Share"));
                                                }
                                            }
                                        });
                                        helper.load();
                                    }
                                } else if (type.equalsIgnoreCase("video")){
                                    if(item.has("file_url")){
                                        Uri uri = Uri.parse(item.getString("file_url"));
                                        callback_intent.putExtra(Intent.EXTRA_STREAM, uri);
                                        // override with image type if one of the items is an image
                                        callback_intent.setType("video/*");
                                        callback_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        counter++;
                                        if (counter == l) {
                                            JasonHelper.next("success", action, new JSONObject(), event, context);
                                            context.startActivity(Intent.createChooser(callback_intent, "Share"));
                                        }
                                    }
                                } else if (type.equalsIgnoreCase("audio")){
                                    if(item.has("file_url")){
                                        Uri uri = Uri.parse(item.getString("file_url"));
                                        callback_intent.putExtra(Intent.EXTRA_STREAM, uri);
                                        callback_intent.setType("audio/*");
                                        callback_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        counter++;
                                        if (counter == l) {
                                            JasonHelper.next("success", action, new JSONObject(), event, context);
                                            context.startActivity(Intent.createChooser(callback_intent, "Share"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        }).start();
    }
}
