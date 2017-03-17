var lat = Activity.getLatitude();
var lng = Activity.getLongitude();
var map = L.map('map', {zoomControl: false, attribution:'&copy; Leaflet'}).setView([lat, lng], 11);
var timeFromLastTouch = 0;
var locationZoom = 16;
var maxZoom = 16;
var markerLat = 0;
var markerLng = 0;
var centerMapView = true;
var percentage = 0;
var zooming = false;
var routePolyline = null;
var makeNewRoute = false;
var isRecording = false;
var drawingTrack = false;
var routeStrokeColor = "#3fb4fb";
var markerIcon = L.icon({
    iconUrl: 'img/actual_place.png',
    iconSize: [40, 40], // size of the icon
    iconAnchor: [20, 37], // point of the icon which will correspond to marker's location
});
var marker = L.marker([lat, lng], {icon: markerIcon});
var tileLayer = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: maxZoom
}).addTo(map);
var showMyLocRunning = false;

marker.addTo(map);

function detectTouch() {
    // If user holding set time how long don't center map
    if (Activity.isHoldingMap()) {
        timeFromLastTouch = 20000;
    }
    // Update time from last touch
    else if (timeFromLastTouch > 0) {
        timeFromLastTouch -= 200;
    }
}
setInterval(detectTouch, 200);

function currentLocation() {
    var latitude = Activity.getLatitude();
    var longitude = Activity.getLongitude();
    map.setView([latitude, longitude], locationZoom, {pan: {animate: false}});

    timeFromLastTouch = 0;
}

document.getElementById("current_location").addEventListener("click",
        currentLocation);

function continueShowLoc() {
    var latitude = Activity.getLatitude();
    var longitude = Activity.getLongitude();

    marker.setLatLng([latitude, longitude]).update();
    lat = latitude;
    lng = longitude;

    map.setView([latitude, longitude], locationZoom, {pan: {animate: false}});

    timeFromLastTouch = 0;

    showMyLocRunning = false;

    setTimeout(function () {
        if (showMyLocRunning === false) {
            showMyLocation();
        }
    }, 900);
}

function showMyLocation() {
    showMyLocRunning = true;
    centerMapView = true;
    markerLat = lat;
    markerLng = lng;
    lat = Activity.getLatitude();
    lng = Activity.getLongitude();
    isRecording = Activity.isRecording();

    if (markerLat !== lat || markerLng !== lng) {
        centerMapView = false;
        percentage = 0.1;
        var tmpRoutePolyline = null;
        if (isRecording) {
            tmpRoutePolyline = L.polyline([[markerLat, markerLng]],
                    {color: routeStrokeColor, opacity: 1})
                    .addTo(map);
        }

        function animateMarker() {
            if (percentage > 1) {
                makeNewRoute = false;
                // Draw large static route
                drawCurrentTrack(lat, lng);
                if (tmpRoutePolyline !== null) {
                    // Remove animated route line
                    map.removeLayer(tmpRoutePolyline);
                }

                showMyLocation();
            } else {
                if (!zooming) {
                    var pLat = markerLat + percentage * (lat - markerLat);
                    var pLng = markerLng + percentage * (lng - markerLng);
                    // Update marker location
                    marker.setLatLng([pLat, pLng]).update();

                    if (!makeNewRoute && tmpRoutePolyline !== null) {
                        // Draw animated route line segment
                        tmpRoutePolyline.addLatLng([pLat, pLng]);
                    }

                    if (timeFromLastTouch <= 0) {
                        // Pan map view
                        map.setView([pLat, pLng], locationZoom, {
                            pan: {animate: false}
                        });
                    }
                }
                percentage = percentage + 0.1;
                setTimeout(animateMarker, 50);
            }
        }
        animateMarker();
    } else {
       makeNewRoute = false;
        if (timeFromLastTouch <= 0) {
            // Pan map view
            map.setView([lat, lng], locationZoom, {
                pan: {animate: true}
            });
        }
        setTimeout(showMyLocation, 1000);
    }
}

map.on('zoomstart', function (e) {
    zooming = true;
});

map.on('zoomend', function (e) {
    zooming = false;
});

function drawCurrentTrack(lat, lng) {
    if (isRecording) {
        if (routePolyline !== null) {
            routePolyline.addLatLng([lat, lng]);
        }
    }
}

function makeNewTrack() {
    makeNewRoute = true;
    var lat = Activity.getLatitude();
    var lng = Activity.getLongitude();
    routePolyline = L.polyline([[lat, lng]],
            {color: routeStrokeColor, opacity: 1})
            .addTo(map);
}

function rmCurrTrack() {
    if (routePolyline !== null) {
        map.removeLayer(routePolyline);
        routePolyline = null;
    }
}

function loadScript(url, callback) {
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = url;
    script.onreadystatechange = callback;
    script.onload = callback;
    head.appendChild(script);
}

function loadStyleSheet(url, callback) {
    var head = document.getElementsByTagName('head')[0];
    var styleSheet = document.createElement('link');
    styleSheet.rel = 'stylesheet';
    styleSheet.href = url;
    styleSheet.onreadystatechange = callback;
    styleSheet.onload = callback;
    head.appendChild(styleSheet);
}

//map2.js include
var reportPinned = false;
var reportPin;
var reportPinIcon = L.icon({
    iconUrl: 'img/some-place.png',

    iconSize:     [50, 50], // size of the icon
    iconAnchor:   [25, 50], // point of the icon which will correspond to marker's location
});
var reportPinMovedBefore = 0;

map.on('contextmenu', function (e) {
    if (reportPinned === false) {
        reportPin = L.marker(e.latlng, {icon: reportPinIcon, draggable: true});
        reportPin.addTo(map).setZIndexOffset(100);
        reportPinned = true;
        MainActivity.setReportCoord(e.latlng.lat, e.latlng.lng);
        reportPinMovedBefore = 0;

        reportPin.on('move', function (e) {
            MainActivity.setReportCoord(e.latlng.lat, e.latlng.lng);
            reportPinMovedBefore = 0;
        });
    } else {
        reportPin.setLatLng(e.latlng).update();
        MainActivity.setReportCoord(e.latlng.lat, e.latlng.lng);
        reportPinMovedBefore = 0;
    }
});

function removePin() {
    if (reportPinned) {
        if (reportPinMovedBefore < 5000) {
            reportPinMovedBefore += 200;
        } else {
            map.removeLayer(reportPin);
            reportPinned = false;
            MainActivity.setReportPin(false);
        }
    }
}
setInterval(removePin, 200);