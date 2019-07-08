<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@page import="java.util.Map"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


    <div class="panel-body">
      <div id="leafletmap" class="hidden-xs" style="width: 100%; height: 235px;"></div>
    </div>

  <script type="text/javascript">
      var jsonpath = "<%= request.getContextPath() %>/static/json/geos.json";
      var geoData;

    $.getJSON(jsonpath, function(data) {
        geoData = data;
    });

    var map = L.map('leafletmap').setView([27.9,50.6], 2);
        var csvData, csvData2;
        var markersLayer;
        var timer;
        var timerInterval = 5 * 1000;
        var previousLayer = '';

        var layer = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 18,
            minZoom: 2,
            id: 'lib-zzd.cig7yktpl0489unlx2e5ielz9',
            accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
        }).addTo(map);
        // WKU location
        L.marker([27.916388,120.653688]).addTo(map);

        function Point(lat, lng, val) {
            this.latitude = lat;
            this.longitude = lng;
            this.val = val;
        }

        function animateMap() {
            timer = setInterval (function() {
                if(previousLayer !== ''){
                    map.removeLayer(previousLayer);
                }
                createMarkers(geoData);
            }, timerInterval);
        }

        animateMap();

        function step(){
            markersLayer.eachLayer(function(layer) {
                onEachFeature(layer);
            });
        }

        function createMarkers(geoData) {

            var r = 5;

            var markerStyle = {
                radius: r,
                fillColor: "#39F",
            };

            var markersArray = [];

            for (var i=0; i<geoData.length; i++) {
                var feature = {};
                feature.properties = geoData[i];
                var lat = Number(feature.properties.latitude);console.log(i + " latitude="+lat);
                var lng = Number(feature.properties.longitude);
                var marker = L.circleMarker([lat,lng], markerStyle);
                marker.feature = feature;
                markersArray.push(marker);
            };

            //create a markers layer with all of the circle markers
            markersLayer = L.featureGroup(markersArray);
            previousLayer = markersLayer;

            //add the markers layer to the map
            markersLayer.addTo(map);

            // markersLayer.eachLayer(function(layer){
            //     onEachFeature(layer);
            // });
        }

        function onEachFeature(layer) {
            // var area = layer.feature.properties.value * scaleFactor;
            // var radius = Math.sqrt(area/Math.PI);
            // layer.setRadius(radius);
        }


  </script>
