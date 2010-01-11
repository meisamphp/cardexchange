package agh.mobile.contactexchange.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

class ListEntry {
	int id;
	String name;

	public ListEntry(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}

public class PartnersList extends ListActivity {
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("Select a partner to ecchange cards");
		
		HashMap<Integer, String> partners = (HashMap<Integer, String>) getIntent().getExtras().getSerializable("partners");

		ArrayList<ListEntry> list = new ArrayList<ListEntry>();
		for (Entry<Integer, String> e : partners.entrySet())
			list.add(new ListEntry(e.getKey(), e.getValue()));

		setListAdapter(new ArrayAdapter<ListEntry>(this,
				android.R.layout.simple_list_item_1, list));
	} 

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l, v, position, id);

		// Get the item that was clicked
		ListEntry entry = (ListEntry) (this.getListAdapter().getItem(position));
		
    	Intent intent = getIntent();
    	intent.putExtra("partnerId", entry.id);
    	setResult(RESULT_OK, intent);
    	finish();
	} 
}
