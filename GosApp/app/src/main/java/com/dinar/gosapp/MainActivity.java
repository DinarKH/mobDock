package com.dinar.gosapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private String[] scope = new String[]{VKScope.FRIENDS, VKScope.WALL, VKScope.GROUPS, VKScope.AUDIO};
    private ListView listView;
    ArrayList<String> arrayList;
    ArrayAdapter<String> arrayAdapter;
    SQLiteDatabase database;
    DBHelper dbHelper;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();


        // Операции для выбранного пункта меню
        switch (id) {
            case R.id.firstItem:
                final EditText taskEditText = new EditText(this);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Поиск по тексту")
                        .setMessage("Введите текст для поиска")
                        .setView(taskEditText)
                        .setPositiveButton("Поиск", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String task = String.valueOf(taskEditText.getText());
                                ArrayList<String> newListSearch =new ArrayList<String>();
                                for(int i=0;i<arrayList.size();i++){
                                    if(arrayList.get(i).contains(task)){
                                        newListSearch.add(arrayList.get(i));
                                    }
                                }
                                arrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                                        android.R.layout.simple_expandable_list_item_1, newListSearch);
                                listView.setAdapter(arrayAdapter);
                                Toast.makeText(getApplicationContext(), task, Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();
                return true;
        case R.id.secondItem:
            startLoad();

            Toast.makeText(getApplicationContext(), "Загрузка завершена", Toast.LENGTH_LONG).show();
            return true;
        case R.id.dbItem:
            MainActivity.this.deleteDatabase(DBHelper.DATABASE_NAME);
            dbHelper = new DBHelper(this);

            database = dbHelper.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            for(int i=0;i<arrayList.size();i++) {
                contentValues.put(DBHelper.KEY_NAME, arrayList.get(i));
                database.insert(DBHelper.TABLE_CONTACTS, null, contentValues);
            }

            return true;
        case R.id.dbLoad:
            database = dbHelper.getWritableDatabase();
            arrayList = new ArrayList<String>();
            Cursor cursor = database.query(DBHelper.TABLE_CONTACTS, null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
                int nameIndex = cursor.getColumnIndex(DBHelper.KEY_NAME);
                do {
                    arrayList.add(cursor.getString(nameIndex));
                    arrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                            android.R.layout.simple_expandable_list_item_1, arrayList);
                    listView.setAdapter(arrayAdapter);
                    Log.d("mLog", "ID = " + cursor.getInt(idIndex) +
                            ", name = " + cursor.getString(nameIndex));
                } while (cursor.moveToNext());
            } else
                Log.d("mLog","0 rows");

            cursor.close();
            return true;
        case R.id.fileWrite:
            writeFileSD();
            Toast.makeText(getApplicationContext(), "Запись в файл", Toast.LENGTH_LONG).show();
            return true;
        case R.id.fileRead:
            readFileSD();
            Toast.makeText(getApplicationContext(), "Чтение из файла", Toast.LENGTH_LONG).show();
            return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private String DIR_SD = "dir";
    private String FILENAME_SD = "file.txt";
    void writeFileSD() {
        try {
            FileOutputStream fileOutputStream =openFileOutput(FILENAME_SD,MODE_PRIVATE);
            String strToFile="";
            for(int i=0;i<arrayList.size();i++){
                strToFile+=arrayList.get(i)+",";
            }
            Log.d("mLog", strToFile );
            fileOutputStream.write(strToFile.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void readFileSD() {
        try {
            FileInputStream fileInputStream = openFileInput(FILENAME_SD);
            InputStreamReader reader = new InputStreamReader(fileInputStream);
            BufferedReader buffer = new BufferedReader(reader);
            StringBuffer stringBuffer = new StringBuffer();
            String lines;
            while ((lines = buffer.readLine())!=null){
                stringBuffer.append(lines+"\n");
            }
            String readFromFile = stringBuffer.toString();
            String[] arr =readFromFile.split(",");
            for(int i=0;i<arr.length;i++){
                Log.d("mLog", arr[i]);
            }
            Log.d("mLog", stringBuffer.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());
//        System.out.println("hello");
//        System.out.println(Arrays.asList(fingerprints));
        arrayList = new ArrayList<String>();
        listView = (ListView) findViewById(R.id.friendsView);
        dbHelper = new DBHelper(this);
        VKSdk.login(this, scope);
    }

    private void startLoad() {
        final VKRequest request = VKApi.wall().get(VKParameters.from(VKApiConst.OWNER_ID, VKAccessToken.currentToken().userId,
                VKApiConst.COUNT, 50, VKApiConst.EXTENDED, 1));
        arrayList.clear();
        //VKRequest request = new VKRequest("friends.get", VKParameters.from(VKApiConst.FIELDS, "sex,bdate,city"));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    System.out.println(response.responseString);
                    JSONObject jsonObject = (JSONObject) response.json.get("response");
                    JSONArray jsonArray = (JSONArray) jsonObject.get("items");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        jsonObject = (JSONObject) jsonArray.get(i);
                        System.out.println(jsonObject.getString("text"));
                        arrayList.add(jsonObject.getString("text"));
                        arrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                                android.R.layout.simple_expandable_list_item_1, arrayList);
                        listView.setAdapter(arrayAdapter);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(final VKAccessToken res) {
                // Пользователь успешно авторизовался
                listView = (ListView) findViewById(R.id.friendsView);

                //final VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS,"first_name"));
                //друзья
                //final VKRequest request = new VKRequest("friends.get", VKParameters.from(VKApiConst.FIELDS, "nickname"));
                //final VKRequest request = VKApi.groups().get(VKParameters.from(VKApiConst.OWNER_ID,VKAccessToken.currentToken().userId));
                //final VKRequest request = VKApi.wall().get(VKParameters.from(VKApiConst.OWNER_ID, VKAccessToken.currentToken().userId,
                //        VKApiConst.COUNT, 50, VKApiConst.EXTENDED, 1));
                final VKRequest request = VKApi.wall().get(VKParameters.from(VKApiConst.OWNER_ID, VKAccessToken.currentToken().userId,
                               VKApiConst.COUNT, 50, VKApiConst.EXTENDED, 1));
                //стена
                //final VKRequest request = VKApi.wall().get(VKParameters.from("72232004", VKAccessToken.currentToken().userId,
                //                VKApiConst.COUNT, 50, VKApiConst.EXTENDED, 1));
                //final VKRequest request = VKApi.groups().get(VKParameters.from(VKApiConst.OWNER_ID, VKAccessToken.currentToken().userId,
                //                        VKApiConst.COUNT, 50, VKApiConst.EXTENDED, 1));
                //группы
                request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        try {
                            JSONObject jsonObject = (JSONObject) response.json.get("response");
                            JSONArray jsonArray = (JSONArray) jsonObject.get("items");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                jsonObject = (JSONObject) jsonArray.get(i);
                                System.out.println(jsonObject.getString("text"));
                                arrayList.add(jsonObject.getString("text"));
                                arrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                                        android.R.layout.simple_expandable_list_item_1, arrayList);
                                listView.setAdapter(arrayAdapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //группы
                        /*ArrayList<String> arrayList = new ArrayList<String>();
                        try {
                            JSONObject jsonObject = (JSONObject) response.json.get("response");
                            JSONArray jsonArray = (JSONArray) jsonObject.get("items");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                jsonObject = (JSONObject) jsonArray.get(i);
                                System.out.println(jsonObject.getString("name"));
                                arrayList.add(jsonObject.getString("name"));
                                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this,
                                        android.R.layout.simple_expandable_list_item_1, arrayList);
                                listView.setAdapter(arrayAdapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }*/
                        /*VKList vkList = (VKList) response.parsedModel;

                        ArrayAdapter<String> arrayAdapter =new ArrayAdapter<String>(MainActivity.this,
                                android.R.layout.simple_expandable_list_item_1,vkList);
                        listView.setAdapter(arrayAdapter);
                    }
                });
                //Toast.makeText(getApplicationContext(), "Super", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(VKError error) {
                // Произошла ошибка авторизации (например, пользователь запретил авторизацию)
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }*/
}
