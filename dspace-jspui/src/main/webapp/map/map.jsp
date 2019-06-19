<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%--  Leaflet maps  --%>
<c:set var="pieType">location</c:set>
<c:set var="targetDiv" scope="page">div_${data.jspKey}_${statType}_${objectName}_${pieType}</c:set>

<c:set var="jsDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType}_${pieType}</c:set>
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
    var ${ jsDataObjectName } = new Array(${ fn: length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable) });
    <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].dataTable}" var="row" varStatus="status">
      ${jsDataObjectName}[${status.count - 1}]= ['<c:out value="${row.latitude}"/>','<c:out value="${row.longitude}"/>','<c:out value="${row.value}"/>',<c:out value="${row.percentage}"/>];
    </c:forEach >

    function initialize_${ jsDataObjectName }() {
      if (${ jsDataObjectName }.length == 0) return;
      var leafletmap = L.map("${targetDiv}").setView([${ jsDataObjectName }[0][0], ${ jsDataObjectName }[0][1]], 4);


      L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
          '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
          'Imagery &copy; <a href="http://mapbox.com">Mapbox</a>',
        maxZoom: 18,
        minZoom: 4,
        id: 'lib-zzd.cig7yktpl0489unlx2e5ielz9',
        accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
      }).addTo(leafletmap);

      for (var i = 0; i < ${ jsDataObjectName }.length; i++) {
        marker = new L.marker([${ jsDataObjectName }[i][0], ${ jsDataObjectName }[i][1]]).addTo(leafletmap);
      }
  }

  initialize_${ jsDataObjectName } ();
  -->
  </script>
</c:if>