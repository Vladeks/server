package waterloo.com.core;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.maps.android.SphericalUtil;

import com.google.gson.JsonArray;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Narula on 10/24/2015.
 */
public class ParkingFinder {

    public static final String BICYCLE = "bicycle";
    public static final String CARBIKE = "carbike";

    public static void main(String arg[]) {
        ParkingFinder pf = new ParkingFinder();
        LatLng ob = new LatLng(43.4639, -80.5253);
        pf.setCurrentLocation(ob);

        LatLng nob = new LatLng(47.0297103, -65.4625936);
        System.out.println("hello");
        LocationData ob1 = new LocationData();
        ob1.setLocation(nob);
        ob1.setVehicleTypes(1);
        ob1.setDescription("dsfafafssafdf");
        ob1.setAddress("165 glen");
        ob1.setStartDate("2015-10-01");
        ob1.setEndDate("2015-10-05");
        ob1.setIsBicycleParking(false);
        ob1.setIsCarBikeParking(true);
        ob1.setMotorcycleAllowed("Y");
        ob1.setOwnershipType(1);
        ob1.setAmountPerDay(7.0f);
        ob1.setCapacity("2");


        pf.getParkingArea(nob, Constants.BICYCLE);
        System.out.print(pf.getParkingArea(nob, Constants.BICYCLE));
        //pf.postParkingInfo(ob1);

    }

    LatLng currentLocation;

    public ArrayList<LocationData> getParkingArea(LatLng latlng, int vehicle) {
        setCurrentLocation(latlng);
        return populateParkingData(vehicle);
    }

    private ArrayList<LocationData> populateParkingData(int vehicle) {
        ArrayList<LocationData> temp = getResponsefromServer(vehicle);
        return refineLocationData(temp);

    }

    private ArrayList<LocationData> refineLocationData(ArrayList<LocationData> temp) {
        ArrayList<LocationData> locationData = new ArrayList<LocationData>();
        Iterator<LocationData> iterator = temp.iterator();
        while (iterator.hasNext()) {
            LocationData tempLoc = iterator.next();
            if (passCriteria(tempLoc.getLocation()))
                locationData.add(tempLoc);

        }
        return locationData;
    }

    private boolean passCriteria(LatLng location) {
        double distance = SphericalUtil.computeDistanceBetween(getCurrentLocation(), location);
        if (distance <= Constants.radius) {
            return true;
        }
        return false;
    }

    private ArrayList<LocationData> getResponsefromServer(int vehicle) {
        ArrayList<LocationData> allData = new ArrayList<LocationData>();
        String vehicleInfo = getVehicle(vehicle);

        //HttpClient client = new DefaultHttpClient();
        //HttpGet httpGet = new HttpGet("http://ec2-52-26-80-237.us-west-2.compute.amazonaws.com/waterpark/rest/parking/" + vehicleInfo);
        try {
            String url = "http://ec2-52-26-80-237.us-west-2.compute.amazonaws.com/waterpark/rest/parking/" + vehicleInfo;
            URL obj = new URL(url);
            HttpURLConnection client = (HttpURLConnection) obj.openConnection();
            client.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            //System.out.println();
            //sb.toString(
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            JsonArray jArray = gson.fromJson(sb.toString(), JsonArray.class);

            for (int i = 0; i < jArray.size(); i++) {
                JsonObject jObject = jArray.get(i).getAsJsonObject();
                LocationData temp = new LocationData();
                String desc = jObject.get("description").getAsString();
                String lat = jObject.get("latitude").getAsString();
                String lng = jObject.get("longitude").getAsString();
                String address = jObject.get("address").getAsString();
                String capacity = jObject.get("capacity").getAsString();
                String ownershipType = jObject.get("ownershipType").getAsString();
                String id = jObject.get("id").getAsString();

                if(ownershipType.equalsIgnoreCase("PRIVATE")){
                    temp.setEmail(jObject.get("ownerEmail").getAsString());
                    temp.setContact_Num(jObject.get("ownerPhone").getAsString());
                    temp.setAmountPerDay(Float.parseFloat(jObject.get("amountPerDay").getAsString()));
                }

                String mcAllowed;
                if (vehicleInfo.equalsIgnoreCase("carbike")) {
                    mcAllowed = jObject.get("motorcycleAllowed").getAsString();
                    if (mcAllowed.equalsIgnoreCase("Y")) {
                        temp.setVehicleTypes(Constants.MOTOR_CYCLE);
                        temp.setMotorcycleAllowed("Y");
                    } else {
                        temp.setVehicleTypes(Constants.CAR);
                        temp.setMotorcycleAllowed("N");
                    }
                } else {
                    temp.setVehicleTypes(Constants.BICYCLE);
                    temp.setMotorcycleAllowed("N");
                }


                LatLng tempOb = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                temp.setCapacity(capacity);
                temp.setLocation(tempOb);
                temp.setDescription(desc);
                temp.setAddress(address);
                temp.setOwnershipType(matchParking(ownershipType));
                temp.setId(id);


                allData.add(temp);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getDataFromWatServer(allData);
    }

    private ArrayList<LocationData> getDataFromWatServer(ArrayList<LocationData> allData) {
        {
            try {
                LocationData temp = new LocationData();
                String url = "https://api.uwaterloo.ca/v2/parking/watpark.json?key=adf160317a8e94131e83197ddfbafa99";
                URL watUrl = new URL(url);

                HttpURLConnection client = (HttpURLConnection) watUrl.openConnection();
                client.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();

                JsonObject obj = gson.fromJson(sb.toString(), JsonObject.class);
                JsonArray jArray = obj.get("data").getAsJsonArray();

                //JsonArray elem = obj.get("results").getAsJsonArray();
                for (int i = 0; i < jArray.size(); i++) {
                    JsonObject data = jArray.get(i).getAsJsonObject();
                    String Description = data.get("lot_name").getAsString();
                    String lat = data.get("latitude").getAsString();
                    String lng = data.get("longitude").getAsString();
                    String capacity = data.get("capacity").getAsString();
                    //String current_count = data.get("current_count").getAsString();
                    //String percent_filled = data.get("percent_filled").getAsString();
                    //String last_updated = data.get("last_updated").getAsString();

                    LatLng tempOb = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                    temp.setAddress("200 University Avenue : " + Description);
                    temp.setLocation(tempOb);
                    temp.setDescription("200 University Avenue : " + Description);
                    temp.setCapacity(capacity);
                    temp.setOwnershipType(Constants.PRIVATE_PARKING);

                    allData.add(temp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return allData;
        }
    }

    private int matchParking(String parkingType) {
        if (parkingType.equalsIgnoreCase("PRIVATE"))
            return Constants.PRIVATE_PARKING;
        else
            return Constants.PUBLIC_PARKING;
    }

    private String getVehicle(int vehicle) {
        switch (vehicle) {
            case Constants.BICYCLE:
                return BICYCLE;
            case Constants.CAR:
                return CARBIKE;
            default:
                return CARBIKE;
        }

    }

    public LatLng getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LatLng currentLocation) {
        this.currentLocation = currentLocation;
    }


    public boolean postBookingInfo(LocationData locationData) {
        String postUrl = "http://ec2-52-26-80-237.us-west-2.compute.amazonaws.com/waterpark/rest/parking/book";
        URL obj = null;
        try {
            obj = new URL(postUrl);

            HttpURLConnection client = (HttpURLConnection) obj.openConnection();
            client.setRequestMethod("POST");

            client.setRequestProperty("Content-Type", "application/json");
            client.setRequestProperty("Accept", "application/json");

            String urlParameters = locationData.toString();

            client.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(client.getOutputStream());
            wr.writeBytes(urlParameters.toString());
            wr.flush();
            wr.close();

            int responseCode = client.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + postUrl);
            System.out.println("Post parameters : " + urlParameters.toString());
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public boolean postParkingInfo(LocationData locationData) {
        String postUrl = "http://ec2-52-26-80-237.us-west-2.compute.amazonaws.com/waterpark/rest/parking/saveparking";
        URL obj = null;
        try {
            obj = new URL(postUrl);

            HttpURLConnection client = (HttpURLConnection) obj.openConnection();
            client.setRequestMethod("POST");

            client.setRequestProperty("Content-Type", "application/json");
            client.setRequestProperty("Accept", "application/json");

            String urlParameters = locationData.toString();

            client.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(client.getOutputStream());
            wr.writeBytes(urlParameters.toString());
            wr.flush();
            wr.close();

            int responseCode = client.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + postUrl);
            System.out.println("Post parameters : " + urlParameters.toString());
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
