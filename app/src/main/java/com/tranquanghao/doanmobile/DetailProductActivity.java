package com.tranquanghao.doanmobile;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.tranquanghao.Database;
import com.tranquanghao.model.Category;
import com.tranquanghao.model.Product;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class DetailProductActivity extends AppCompatActivity {
    final String DATABASE_NAME = "qlhh.sqlite";
    final int RESQUEST_TAKE_PHOTO = 123;
    final int RESQUEST_CHOOSE_PHOTO = 321;

    ImageView imgProduct;
    TextView txtIdProduct;
    EditText edtName, edtPrice, edtVolumetric, edtProduction;
    Spinner spinnerEditCategory;
    Button btnEdit, btnDel, btnExitEdit;
    Product product;
    private ArrayAdapter<Category> adapterCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_product);

        addControls();
        addEvents();
        getData();
    }

    private void getData() {
        if (getIntent().getExtras() != null) {
            product = (Product) getIntent().getSerializableExtra("EDIT");

            SQLiteDatabase database = Database.initDatabase(this,DATABASE_NAME);
            Cursor cursor = database.rawQuery("SELECT * FROM Product Where idProduct = ?",new String[] {product.getProductId() + ""});
            cursor.moveToFirst();
            String id   = cursor.getString(0);
            String name = cursor.getString(1);
            byte[] image = cursor.getBlob(2);
            Double price = cursor.getDouble(3);
            Integer volumetric = cursor.getInt(4);
            String production = cursor.getString(5);
            String idCategory = cursor.getString(6);
            cursor.close();

            Cursor cursor2 = database.rawQuery("SELECT * FROM Category Where idCategory = ?", new String[] {idCategory + ""});
            cursor2.moveToFirst();
            String idCategory2   = cursor2.getString(0);
            String nameCategory  = cursor2.getString(1);
            Category category = new Category(idCategory2, nameCategory);
            cursor2.close();

            //Bitmap image
            Bitmap bitmap = BitmapFactory.decodeByteArray(image,0,image.length);

            //Day len giao dien
            imgProduct.setImageBitmap(bitmap);
            txtIdProduct.setText(id);
            edtName.setText(name);
            edtPrice.setText(String.valueOf(price));
            edtVolumetric.setText(String.valueOf(volumetric));
            edtProduction.setText(production);
            spinnerEditCategory.setSelection(adapterCategory.getPosition(category));
        }
    }

    private void editProduct(){
        String id = product.getProductId();
        String name = edtName.getText().toString();
        Double price = Double.parseDouble(edtPrice.getText().toString());
        Integer volumetric = Integer.parseInt(edtVolumetric.getText().toString());
        String production = edtProduction.getText().toString();
        Category category = (Category) spinnerEditCategory.getSelectedItem();
        byte[] image = convertImageViewToByte(imgProduct);

        ContentValues contentValues = new ContentValues();
        contentValues.put("idProduct",id);
        contentValues.put("name",name);
        contentValues.put("image",image);
        contentValues.put("price",price);
        contentValues.put("volumetric",volumetric);
        contentValues.put("production",production);
        contentValues.put("idCategory",category.getCategoryId());

        SQLiteDatabase database = Database.initDatabase(DetailProductActivity.this, DATABASE_NAME);
        database.update("Product",contentValues,"idProduct = ?",new String[] {id + ""} );

        Product productEdit = new Product(id,name,image,category,price,volumetric,production);

        Intent intent = new Intent(DetailProductActivity.this, MainActivity.class);
        intent.putExtra("EDIT", productEdit);
        setResult(200, intent);
        finish();
    }

    private void addEvents() {
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder  alert = new AlertDialog.Builder(DetailProductActivity.this);
                alert.setIcon(android.R.drawable.ic_menu_edit);
                alert.setTitle(DetailProductActivity.this.getText(R.string.simple_editTitle));
                alert.setMessage(DetailProductActivity.this.getText(R.string.simple_editMessage));
                alert.setPositiveButton(DetailProductActivity.this.getText(R.string.simple_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editProduct();
                    }
                });
                alert.setNegativeButton(DetailProductActivity.this.getText(R.string.simple_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog dialog = alert.create();
                dialog.show();
            }
        });

        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder  alert = new AlertDialog.Builder(DetailProductActivity.this);
                alert.setIcon(android.R.drawable.ic_delete);
                alert.setTitle(DetailProductActivity.this.getText(R.string.simple_removeTitle));
                alert.setMessage(DetailProductActivity.this.getText(R.string.simple_removeMessage));
                alert.setPositiveButton(DetailProductActivity.this.getText(R.string.simple_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //yes xoa
                        deleteProduct(product.getProductId());
                    }
                });
                alert.setNegativeButton(DetailProductActivity.this.getText(R.string.simple_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog dialog = alert.create();
                dialog.show();
            }
        });

        btnExitEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        imgProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence image[] = new CharSequence[] {DetailProductActivity.this.getText(R.string.simple_photo),
                        DetailProductActivity.this.getText(R.string.simple_camera)};
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailProductActivity.this);
                builder.setTitle(DetailProductActivity.this.getText(R.string.simple_chooseImage));
                builder.setItems(image, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0){
                            choosePhoto();
                        }else if (which == 1){
                            takePicture();
                        }
                    }
                });
                builder.show();
            }
        });
    }

    private void deleteProduct(String idProduct) {
        SQLiteDatabase database = Database.initDatabase(this,DATABASE_NAME);
        database.delete("Product","idProduct = ?", new String[] {idProduct + ""});

        Intent intent = new Intent(DetailProductActivity.this, MainActivity.class);
        intent.putExtra("DEL", product.getProductId());
        setResult(300, intent);
        finish();
    }

    private void addControls() {
        imgProduct   = (ImageView) findViewById(R.id.imgEditProduct);
        txtIdProduct = (TextView) findViewById(R.id.txtEditId);
        edtPrice = (EditText) findViewById(R.id.edtEditPrice);
        edtName = (EditText) findViewById(R.id.edtEditName);
        edtVolumetric = (EditText) findViewById(R.id.edtEditVolumetric);
        edtProduction = (EditText) findViewById(R.id.edtEditProduction);
        spinnerEditCategory = (Spinner) findViewById(R.id.spinnerEditCategory);

        btnDel = (Button) findViewById(R.id.btnDel);
        btnEdit = (Button) findViewById(R.id.btnEdit);
        btnExitEdit = (Button) findViewById(R.id.btnExitEdit);

        adapterCategory = new ArrayAdapter<Category>(DetailProductActivity.this, android.R.layout.simple_list_item_1, Data.listDataCategory);
        adapterCategory.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinnerEditCategory.setAdapter(adapterCategory);
    }


    public byte[] convertImageViewToByte(ImageView img){
        BitmapDrawable drawable = (BitmapDrawable) img.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
        byte[] bytes = stream.toByteArray();
        return bytes;
    }

    private void takePicture(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, RESQUEST_TAKE_PHOTO);
    }

    private void choosePhoto(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, RESQUEST_CHOOSE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            if (requestCode == RESQUEST_CHOOSE_PHOTO){
                try {
                    Uri imageUri = data.getData();
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    imgProduct.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }else if(requestCode == RESQUEST_TAKE_PHOTO) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imgProduct.setImageBitmap(bitmap);
            }
        }
    }
}
