<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%--  Leaflet maps  --%>
<c:set var="pieType">location</c:set>
<c:set var="pieType2">mapLocation</c:set>

<c:set var="targetDiv" scope="page">div_${data.jspKey}_${statType}_${objectName}_${pieType}</c:set>

<c:set var="jsDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType}_${pieType}</c:set>
<c:set var="jsMapDataObjectName" scope="page"> data_${statType}_${mapObjectName}_${pieType2}_${pieType2}</c:set>

<c:if test="${fn:length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable) gt 0}">
  <div class="panel panel-default">
    <div class="panel-heading">
      <h6 class="panel-title"><i class="fa fa-map-marker"></i>
        <fmt:message key="view.stats.map.title" />
      </h6>
    </div>
    <div class="panel-body">
      <div id="${targetDiv}" style="width: 100%; height: 300px;"></div>
    </div>
  </div>

  <script type="text/javascript">
<!--
    var dotIconPath = "<%= request.getContextPath()%>/static/css/leaflet/images/doticon.png";
    var ${jsDataObjectName} = new Array(${fn:length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable)});
    var ${jsMapDataObjectName} = new Array(${fn:length(data.resultBean.dataBeans[statType][mapObjectName][pieType2].dataTable)});
    <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].dataTable}" var="row" varStatus="status">
      ${jsDataObjectName}[${status.count - 1}]= ['<c:out value="${row.latitude}"/>','<c:out value="${row.longitude}"/>','<c:out value="${row.value}"/>',<c:out value="${row.percentage}"/>];
    </c:forEach>

    <c:forEach items="${data.resultBean.dataBeans[statType][mapObjectName][pieType2].dataTable}" var="row" varStatus="status2">
      ${jsMapDataObjectName}[${status2.count - 1}]= ['<c:out value="${row.latitude}"/>','<c:out value="${row.longitude}"/>','<c:out value="${row.city}"/>','<c:out value="${row.countryCode}"/>'];
    </c:forEach>

    var dotIcon = new L.icon({
        iconUrl: dotIconPath,
        iconSize: [10, 10]
   });

    function initialize_${jsMapDataObjectName}() {
      if (${jsMapDataObjectName}.length == 0) return;
      var leafletmap = L.map("${targetDiv}").setView([${jsMapDataObjectName}[0][0], ${jsMapDataObjectName}[0][1]], 4);

      L.tileLayer('https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
          '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
          'Imagery &copy; <a href="http://mapbox.com">Mapbox</a>',
        maxZoom: 18,
        minZoom: 2,
        accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
     }).addTo(leafletmap);

      for (var i = 0; i < ${jsMapDataObjectName}.length; i++) {
          marker = new L.marker([${jsMapDataObjectName}[i][0], ${jsMapDataObjectName}[i][1]],
          {
            icon: dotIcon,
          }).addTo(leafletmap)
          .bindPopup(${jsMapDataObjectName}[i][2] + ', ' + ${jsMapDataObjectName}[i][3] );
     }
 }

  initialize_${jsMapDataObjectName}();
-->
  </script>
</c:if>