<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@page import="java.util.Map"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>



<div class="panel-body hidden-xs">
    <div id="leafletmap" class="col-md-9" style="height:235px;"></div>
    <div class="">
        <div class="container" id="readmap" style="overflow-y:auto;height:235px;"></div>
    </div>
</div>

<script type="text/javascript">
    var jsonpath = "<%= request.getContextPath() %>/static/json/geos.json";
    var geoData;

    $.getJSON(jsonpath, function (data) {
        geoData = data;
    });

    var map = L.map('leafletmap').setView([27.9, 18.4], 2);
    var markersLayer;
    var timer;
    var timerInterval = 250 * 1000;
    var timerInterval_init = 1 * 1000;
    var previousLayer = '';
    var init_start = false;

    var layer = L.tileLayer('https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> ',
        maxZoom: 18,
        minZoom: 2,
        id: 'lib-zzd.cig7yktpl0489unlx2e5ielz9',
        accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
    }).addTo(map);
    // WKU location
    // L.marker([27.916388,120.653688]).addTo(map);

    function Point(lat, lng, val) {
        this.latitude = lat;
        this.longitude = lng;
        this.val = val;
    }

    function foo() {
        if (!init_start) {
            timer = setInterval(function () {
                if (previousLayer !== '') {
                    map.removeLayer(previousLayer);
                }
                createMarkers(geoData);
            }, timerInterval_init);
            init_start = true;
        }
    }
    function animateMap() {
        timer = setInterval(function () {
            if (previousLayer !== '') {
                map.removeLayer(previousLayer);
            }
            createMarkers(geoData);
        }, timerInterval);
    }

    //foo();
    animateMap();

    function step() {
        markersLayer.eachLayer(function (layer) {
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

        $("#readmap").empty();

        for (i in geoData) {
            var feature = {};
            feature.properties = geoData[i];
            var lat = Number(feature.properties.latitude);
            var lng = Number(feature.properties.longitude);
            var city = feature.properties.city;
            var countryName = feature.properties.countryName;
            var countryCode = feature.properties.countryCode;
            if(countryCode.toLowerCase()=='tw'){
                countryCode = 'CN';
                countryName = countryName + ', China';
            }
            var marker = L.circleMarker([lat, lng], markerStyle);
            marker.feature = feature;
            marker.bindPopup(city + ', ' + countryName);
            markersArray.push(marker);
            $("#readmap").append(readarea(countryCode, city, countryName));
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

    var readarea = function(countryCode, city, countryName) {
        var info1, info2;
        var arr = splitstring(city, countryName);
        info1 = arr[0];
        if(arr.length > 1) {
            info2 = arr[1];
        }

        var readship1 =
            "<div>" +
                "<div class='col-md-4' style='margin-right:-70px;'>" +
                    "<div class='flag-wrapper'>" +
                        "<div class='img-thumbnail flag flag-icon-background flag-icon-"+ countryCode.toLowerCase() + "'></div>" +
                    "</div>" +
                "</div>" +
                "<div class='col-md-8'>" +
                    "<span style=' padding-left:10px; white-space:nowrap'>" + info1 + "</span>" +
                "</div>" +
                "</div><br />";

        var readship2 =
            "<div>" +
                "<div class='col-md-4' style='margin-right:-70px;'>" +
                    "<div class='flag-wrapper'>" +
                        "<div class='img-thumbnail flag flag-icon-background flag-icon-"+ countryCode.toLowerCase() + "'></div>" +
                    "</div>" +
                "</div>" +
                "<div class='col-md-8'>" +
                    "<span style=' padding-left:10px; white-space:nowrap'>" + info1 + "</span>" +
                "</div>" +
                "</div><br />" +
                "<div class='col-md-8'>" +
                    "<span style=' padding-left:10px; white-space:nowrap'>" + info2 + "</span>" +
                "</div>" +
            "</div><br />";

        if(info2){
            return readship2;
        } else {
            return readship1;
        }
    }

    var splitstring = function(city, countryName) {
        if(city=='undefined' || city==null)
            city = 'undefined, defined undefined tut';
        var loc = city + ", " + countryName;
        var len = loc.length;
        var info, substring = "", prestring = "", arr = [], separator = " ", limit = 35, i = 0;
        var res = loc.split(separator);

        while (substring.length < limit && res.length > i) {
            substring = substring.concat(separator).concat(res[i]);
            if(substring.length < limit){
                prestring = substring;
            }
            i++;
        }

        if(len > limit) {
            info = loc.substring(prestring.length);
            arr.push(prestring);
            arr.push(info);
        } else{
            arr.push(loc);
        }
        return arr;
    }
</script>
