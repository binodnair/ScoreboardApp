package com.binod.scoreboard;
import com.google.android.material.button.MaterialButtonToggleGroup;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;

public class ListMatches extends AppCompatActivity {
    private String leagueName = "";
    private ArrayList matchArray;
    public static int updateMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_matches);

        TableLayout table = findViewById(R.id.matchListTable);
        table.removeAllViews();
        TableRow tableRow = new TableRow(getApplicationContext());
        table.addView(tableRow);
        TextView tv = new TextView(getApplicationContext());
        tv.setText("Select a League from above to see the matches");
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
        tableRow.addView(tv);

        MaterialButtonToggleGroup leagueButtonGroup =  findViewById(R.id.leagueButtonGroup);
        leagueButtonGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener(){
            @Override
            public void onButtonChecked (MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.btnFWCWL) {
                        leagueName = "FWCWL";
                    }
                    else if (checkedId == R.id.btnFT20) {
                        leagueName = "FT20";
                    }
                    else if (checkedId == R.id.btnTPL) {
                        leagueName = "TampaCricket";
                    }
                    else if (checkedId == R.id.btnTCL) {
                        leagueName = "TCL";
                    }
                    Utils.writeLog("Selected League: " + leagueName);
                    TableLayout table = findViewById(R.id.matchListTable);
                    table.removeAllViews();
                    TableRow tableRow = new TableRow(getApplicationContext());
                    table.addView(tableRow);
                    TextView tv = new TextView(getApplicationContext());
                    tv.setText("Getting the list of matches for " + leagueName + ". Please wait....");
                    tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
                    tableRow.addView(tv);
                    new RetrieveMatchList().execute();
                }
            }
        });
    }
    private void refreshScreen() {
        TableLayout table = findViewById(R.id.matchListTable);
        table.removeAllViews();
        table.setBackground(getDrawable(R.drawable.border1));

        TableRow.LayoutParams paramsExample = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT,1.0f);
        boolean oddRow = true;
        for (int i=0; i<matchArray.size(); i++) {
            MatchDetails matchDetails = (MatchDetails) matchArray.get(i);
            TableRow tableRow = new TableRow(getApplicationContext());
            tableRow.setPadding(10,10,10,10);
            // alternate row colors
            if (oddRow) {
                tableRow.setBackground(getDrawable(R.drawable.border1));
            }
            else {
                tableRow.setBackground(getDrawable(R.drawable.border2));
            }
            table.addView(tableRow);
            TextView tv = new TextView(getApplicationContext());
            tv.setLayoutParams(paramsExample);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(getResources().getFont(R.font.roboto));
            //tv.setText(i+1 +". " + matchDetails.getTitle());
            tv.setText(matchDetails.getTitle());
            Button button = new Button(getApplicationContext());
            //button.setText(matchDetails.matchId + ", " + matchDetails.clubId);
            button.setLayoutParams(new TableRow.LayoutParams(250, 100));
            button.setBackground(getDrawable(R.drawable.load_match));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadMatchClicked(matchDetails.matchId, matchDetails.clubId);
                }
            });
            tableRow.setGravity(Gravity.CENTER_VERTICAL);
            tableRow.addView(tv);
            tableRow.addView(button);
            oddRow = !oddRow;
        }
        matchArray = null;
    }

    private void loadMatchClicked(int matchId, int clubId) {
        //Toast.makeText(this, "This Match: : " + matchId + ", " + clubId, Toast.LENGTH_SHORT).show();
        StartScreen.leagueName = leagueName;
        StartScreen.matchId = matchId;
        StartScreen.clubId = clubId;
        StartScreen.ignoreConfig = true;
        StartScreen.updateMode = ListMatches.updateMode;
        Intent intent = new Intent(this, StartScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public class MatchDetails {
        public String matchDate;
        public String teams;
        public int matchId;
        public int clubId;
        public MatchDetails(String matchDate, String teams, int matchId, int clubId) {
            this.matchDate = matchDate;
            this.teams = teams;
            this.matchId = matchId;
            this.clubId = clubId;
        }

        public String getTitle() {
            return "[" + this.matchDate + "] " + teams;
        }
    }
    private class RetrieveMatchList extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String accessURL = "https://cricclubs.com/" + leagueName + "/listMatches.do";
            Utils.writeLog("Pulling data from URL: " + accessURL);
            Document doc = null;
            String webErrMsg = null;
            try {
                doc = Jsoup.connect(accessURL).get();
                scrapePage(doc);
                doc = null;
                return null;
            }
            catch (Exception e) {
                String m = "Score fetch: " + e.getMessage();
                Utils.writeError(m);
                webErrMsg = e.getMessage();
                doc = null;
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            refreshScreen();
        }
        private void scrapePage(Document doc) {
            if (doc == null) return;
            Utils.writeLog("Extracting match listing");

            try {
                Elements elements = doc.getElementsByClass("month-all listView");
                int matchCount = 0;
                matchArray = new ArrayList();
                // pull Team details
                for (Element element: elements) {
                    try {
                        String dd = element.select("h2").get(0).text();
                        String mm = element.select("h5").get(1).text();
                        String matchDate = dd+"-"+mm;
                        matchDate = matchDate.replace(" ", "-");
                        String teams = element.select("h3").get(0).text();
                        Element scoreLink = element.getElementsByClass("list-inline").get(1);
                        String strLink = scoreLink.html();
                        String matchId = strLink.substring(strLink.indexOf("matchId=")+8,strLink.indexOf("&amp"));
                        String clubId  = strLink.substring(strLink.indexOf("clubId=")+7,strLink.indexOf("\" class"));

                        int intMatchId = Integer.parseInt(matchId);
                        int intClubId = Integer.parseInt(clubId);
                        //Utils.writeLog("This Match: " + thisMatch);
                        //Utils.writeLog("LINK: " + strLink);
                        //Utils.writeLog("Match ID: " + matchId);
                        //Utils.writeLog("Club  ID: " + clubId);
                        matchArray.add(new MatchDetails(matchDate, teams, intMatchId, intClubId));
                        // limit to 30 matches in display
                        if (++matchCount > 30) break;
                    }
                    catch (Exception ex) {
                        Utils.writeError("Inner ERROR: " + ex.getMessage());
                    }
                }
                Utils.writeLog("Match listing retrieved successfully");
            } catch (Exception e) {
                String m1 = "Page scrape Error: " + e.getMessage();
                String m2 = "Page scrape Localized Error: " + e.getLocalizedMessage();
                Utils.writeError(m1);
                Utils.writeError(m2);
            }
        }
    }
}