package com.yassirh.digitalocean.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.yassirh.digitalocean.model.Image;

public class ImageDao extends SqlDao<Image> {

	private DatabaseHelper databaseHelper;

	public ImageDao(DatabaseHelper databaseHelper) {
		this.databaseHelper = databaseHelper;
	}

	public long create(Image image) {
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(ImageTable.ID, image.getId());
		values.put(ImageTable.NAME, image.getName());
		values.put(ImageTable.DISTRIBUTION, image.getDistribution());
		values.put(ImageTable.SLUG, image.getSlug());
		values.put(ImageTable.PUBLIC, image.isPublic() ? 1 : 0);
		long id = db.insertWithOnConflict(getTableHelper().TABLE_NAME, null, values,SQLiteDatabase.CONFLICT_REPLACE);
		db.close();
		return id;
	}	

	public Image newInstance(Cursor c) {
		Image image = new Image();
		image.setId(c.getLong(c.getColumnIndex(ImageTable.ID)));
		image.setName(c.getString(c.getColumnIndex(ImageTable.NAME)));
		image.setDistribution(c.getString(c.getColumnIndex(ImageTable.DISTRIBUTION)));
		image.setSlug(c.getString(c.getColumnIndex(ImageTable.SLUG)));
		image.setPublic(c.getInt(c.getColumnIndex(ImageTable.PUBLIC)) > 0);
		return image;
	}

	@Override
	public DatabaseHelper getDatabaseHelper() {
		return this.databaseHelper;
	}

	@Override
	public TableHelper getTableHelper() {
		return new ImageTable();
	}

	public List<Image> getSnapshotsOnly(Object object) {
		SQLiteDatabase db = getDatabaseHelper().getReadableDatabase();

		List<Image> snapshots = new ArrayList<Image>();
		Cursor cursor = db.query(getTableHelper().TABLE_NAME,
				getTableHelper().getAllColumns(), ImageTable.PUBLIC + " = " + "0", null, null, null, null);
		
		if(cursor.moveToFirst()){
			while (!cursor.isAfterLast()) {
				Image snapshot = newInstance(cursor);
				snapshots.add(snapshot);
				cursor.moveToNext();
			}
		}
		cursor.close();
		db.close();
		return snapshots;
	}
	
	public List<Image> getImagesOnly(Object object) {
		SQLiteDatabase db = getDatabaseHelper().getReadableDatabase();

		List<Image> images = new ArrayList<Image>();
		Cursor cursor = db.query(getTableHelper().TABLE_NAME,
				getTableHelper().getAllColumns(), ImageTable.PUBLIC + " = " + "1", null, null, null, null);
		
		if(cursor.moveToFirst()){
			while (!cursor.isAfterLast()) {
				Image image = newInstance(cursor);
				images.add(image);
				cursor.moveToNext();
			}
		}
		cursor.close();
		db.close();
		return images;
	}
}