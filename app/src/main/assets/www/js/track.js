var iconNames = JSON.parse(Activity.getIconNames());

var map = L.map('map', {
    zoomControl: false,
    attributionControl: false
}).setView([51.2194, 4.4025], 11);

L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {maxZoom: 16, })
        .addTo(map);

var track = new L.KML(Activity.getTrackKmlFile(), {async: true}, iconNames);

track.on("loaded", function (e) {
    map.removeLayer(track);
    var track2 = new L.KML(Activity.getTrackKmlFile(), {async: true}, iconNames);
    track2.on("loaded", function (e) {
        map.fitBounds(e.target.getBounds(), {padding: [45, 45]});
    });
    map.addLayer(track2);

});

map.addLayer(track);

var markerIcon = L.icon({
    iconUrl: 'img/actual_place.png',
    iconSize: [40, 40], // size of the icon
    iconAnchor: [20, 37], // point of the icon which will correspond to marker's location
});
var lat = Activity.getLatitude();
var lng = Activity.getLongitude();
var markerLat = lat;
var markerLng = lng;
var zooming = false;
var marker = L.marker([markerLat, markerLng], {icon: markerIcon});

setTimeout(function () {
    marker.addTo(map);
}, 400);

map.on('zoomstart', function (e) {
    zooming = true;
});

map.on('zoomend', function (e) {
    zooming = false;
});

function continueShowLoc() {
    var latitude = Activity.getLatitude();
    var longitude = Activity.getLongitude();

    marker.setLatLng([latitude, longitude]).update();
    showMyLocation();
}

function showMyLocation() {
    markerLat = lat;
    markerLng = lng;
    lat = Activity.getLatitude();
    lng = Activity.getLongitude();

    if (markerLat !== lat || markerLng !== lng) {
        percentage = 0.1;
        function animateMarker() {
            if (percentage > 1) {
                showMyLocation();
            } else {
                if (!zooming) {
                    var pLat = markerLat + percentage * (lat - markerLat);
                    var pLng = markerLng + percentage * (lng - markerLng);
                    // Update marker location
                    marker.setLatLng([pLat, pLng]).update();
                }
                percentage = percentage + 0.1;
                setTimeout(animateMarker, 50);
            }
        }
        animateMarker();
    }
    else {
        setTimeout(showMyLocation, 1000);
    }
}

continueShowLoc();