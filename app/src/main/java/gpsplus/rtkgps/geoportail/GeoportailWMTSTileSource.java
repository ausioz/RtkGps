package gpsplus.rtkgps.geoportail;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;


public class GeoportailWMTSTileSource extends OnlineTileSourceBase {

    private GeoportailLayer mLayer;
    // private static String baseUrl[] = {"https://wxs.ign.fr/"+License.KEY+"/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&STYLE=normal&TILEMATRIXSET=PM"  };

    public GeoportailWMTSTileSource(final String resourceId, GeoportailLayer layer) {
        super("Geoportail", 0, 19, 256, ".png", getUrl());
        this.mLayer = layer;
    }


    private  static String[] getUrl() {
        return new String[]{"https://wxs.ign.fr/"+License.getKey()+"/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&STYLE=normal&TILEMATRIXSET=PM"
        };
    }

    @Override
    public String getTileURLString(long pMapTileIndex) {
        int zoom = MapTileIndex.getZoom(pMapTileIndex);
        int x = MapTileIndex.getX(pMapTileIndex);
        int y = MapTileIndex.getY(pMapTileIndex);

        return getUrl()[0] +
                "&LAYER=" + mLayer.getLayer() +
                "&FORMAT=" + mLayer.getFormat() +
                "&TILEMATRIX=" + zoom +
                "&TILECOL=" + x +
                "&TILEROW=" + y;
    }

}
