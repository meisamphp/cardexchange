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


/**
 * Android activity presenting list of potential partners received from
 * Card Exchange server. When finishing, this activity returns result with
 * id of selected partner. The intent, which called this activity must
 * containt extra field "partners" with serialized HashMap containing partners
 * list.
 * 
 * @author Witold Sowa <witold.sowa@gmail.com>
 */
public class PartnersList extends ListActivity {
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set view title
		setTitle("Select a partner to exchange cards");
		
		// get partners list
		HashMap<Integer, String> partners = (HashMap<Integer, String>) getIntent().getExtras().getSerializable("partners");

		// add partners to the list
		ArrayList<ListEntry> list = new ArrayList<ListEntry>();
		for (Entry<Integer, String> e : partners.entrySet())
			list.add(new ListEntry(e.getKey(), e.getValue()));

		// set the list as a adapter for ListView
		setListAdapter(new ArrayAdapter<ListEntry>(this,
				android.R.layout.simple_list_item_1, list));
	} 

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l, v, position, id);

		// Get the item that was clicked
		ListEntry entry = (ListEntry) (this.getListAdapter().getItem(position));
		
		// set the result
    	Intent intent = getIntent();
    	intent.putExtra("partnerId", entry.id);
    	setResult(RESULT_OK, intent);
    	finish();
	} 
}
