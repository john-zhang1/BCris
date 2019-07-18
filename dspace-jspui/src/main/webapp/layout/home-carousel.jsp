<div id="home-carousel" class="carousel slide" data-ride="carousel">
    <!-- Indicators -->
    <ol class="carousel-indicators">
        <li data-target="#home-carousel" data-slide-to="0" class=""></li>
        <li data-target="#home-carousel" data-slide-to="1" class=""></li>
        <li data-target="#home-carousel" data-slide-to="2" class=""></li>
        <li data-target="#home-carousel" data-slide-to="3" class="active"></li>
    </ol>
    <!-- Wrapper for slides -->
    <div class="carousel-inner" role="listbox">
        <div class="item active">
            <img src="<%= request.getContextPath()%>/image/home-carousel/img-1.png">
            <div class="container">
                <div class="carousel-caption">
                    <p>Wenzhou-Kean University | To Build a Model for a Sino-Foreign Cooperative University.</p>
                </div>
            </div>
        </div>
        <div class="item">
            <img src="<%= request.getContextPath()%>/image/home-carousel/img-2.png">
            <div class="container">
                <div class="carousel-caption">
                    <p>Wenzhou-Kean University (WKU) Returned Overseas Chinese Federation & Overseas Students and Family Association is Established.</p>
                </div>
            </div>
        </div>
        <div class="item">
            <img src="<%= request.getContextPath()%>/image/home-carousel/img-3.png">
            <div class="container">
                <div class="carousel-caption">
                    <p>Stay in Wenzhou, Share the World: Start A World Culture Journey with WKU.</p>
                </div>
            </div>
        </div>
        <div class="item">
            <img src="<%= request.getContextPath()%>/image/home-carousel/img-4.png">
            <div class="container">
                <div class="carousel-caption">
                    <p>The first session of Sino-foreign Cooperative University Association Leadership Summit was held in WKU.</p>
                </div>
            </div>
        </div>
    </div>
    <!-- Controls -->
    <a class="left carousel-control" href="#home-carousel" role="button" data-slide="prev">
        <span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>
        <span class="sr-only">Previous</span>
    </a>
    <a class="right carousel-control" href="#home-carousel" role="button" data-slide="next">
        <span class="glyphicon glyphicon-chevron-right" aria-hidden="true"></span>
        <span class="sr-only">Next</span>
    </a>
</div>