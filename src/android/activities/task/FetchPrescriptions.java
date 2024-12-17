package com.appdinx.cardlink.activities.task;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.appdinx.cardlink.BuildConfig;
import com.appdinx.cardlink.activities.CardInsertionActivity;
import com.appdinx.cardlink.activities.PrescriptionRedeemActivity;
import com.appdinx.cardlink.artemis.Broker;
import com.appdinx.cardlink.prescription.Prescription;

public class FetchPrescriptions extends AsyncTask<Void, Void, List<Prescription>> {

    ProgressDialog pdLoading;
    private final WeakReference<CardInsertionActivity> contextRef;

    private byte[] payload;

    public FetchPrescriptions(CardInsertionActivity context, byte[] payload) {
        this.pdLoading = new ProgressDialog(context);
        this.contextRef = new WeakReference<>(context);
        this.payload = payload;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //this method will be running on UI thread
        pdLoading.setMessage("\tLade E-Rezepte");
        pdLoading.show();
    }

    @Override
    protected List<Prescription> doInBackground(Void... voids) {
        if (BuildConfig.MOCK_PRESCRIPTIONS) {
            return getPrescriptions(true);
        } else {
            return getPrescriptions(false);
        }
    }

    @Override
    protected void onPostExecute(List<Prescription> result) {
        super.onPostExecute(result);
        if (result.isEmpty()) {
            return;
        }
        PrescriptionRedeemActivity.prescriptions.clear();
        PrescriptionRedeemActivity.prescriptions.addAll(result);
        pdLoading.dismiss();
        contextRef.get().runOnUiThread(contextRef.get()::showScanningCompletedDialog);
    }

    @SuppressLint("TrulyRandom")

    @NonNull
    public List<Prescription> getPrescriptions(boolean useMockData) {
        List<Prescription> loadedPrescriptions = new ArrayList<>();
        InputStream is = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp;

            if (useMockData) {
                Resources res = contextRef.get().getResources();
                xpp = res.getXml(com.appdinx.cardlink.R.xml.task_with_pnw);
            } else {
                JSONObject jObject = new JSONObject(new String(payload));

                is = new ByteArrayInputStream(jObject.getString("tokens").getBytes());
                xpp = factory.newPullParser();
                xpp.setInput(is, "UTF_8");
            }

            loadedPrescriptions = parseXmlPrescriptions(xpp);
        } catch (XmlPullParserException | JSONException | IOException e) {
            handlePrescriptionLoadingError(e);
        } finally {
            closeInputStreamQuietly(is);
        }
        return loadedPrescriptions;
    }

    private void handlePrescriptionLoadingError(Exception e) {
        Log.e("PRESCRIPTION", "Could not load prescriptions", e);
        contextRef.get().setStatusText("Es ist ein Fehler beim Abrufen der Rezepte aufgetreten.");
        pdLoading.dismiss();
    }

    private void closeInputStreamQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Prescription> parseXmlPrescriptions(XmlPullParser xpp) throws XmlPullParserException, IOException {
        List<Prescription> loadedPrescriptions = new ArrayList<>();
        String prescriptionId = "";
        String accessCode = "";
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && Objects.equals(xpp.getName(), "identifier")) {
                String systemValue = "";
                String value = "";

                while (!(eventType == XmlPullParser.END_TAG && Objects.equals(xpp.getName(), "identifier"))) {
                    if (eventType == XmlPullParser.START_TAG && Objects.equals(xpp.getName(), "system")) {
                        systemValue = xpp.getAttributeValue(null, "value");
                    } else if (eventType == XmlPullParser.START_TAG && Objects.equals(xpp.getName(), "value")) {
                        value = xpp.getAttributeValue(null, "value");
                    }
                    eventType = xpp.next();
                }

                if (Objects.equals(systemValue, "https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_PrescriptionId")) {
                    prescriptionId = value;
                } else if (Objects.equals(systemValue, "https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_AccessCode")) {
                    accessCode = value;
                }
            } else if (eventType == XmlPullParser.END_TAG && Objects.equals(xpp.getName(), "entry")) {
                Prescription prescription = new Prescription(prescriptionId, accessCode);
                loadedPrescriptions.add(prescription);
            }

            eventType = xpp.next();
        }

        return loadedPrescriptions;
    }
}
