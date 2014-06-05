package org.esa.beam.dataio.mod35;

import org.esa.beam.dataio.netcdf.Modis35ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.Modis35ProfileInitPartReader;
import org.esa.beam.dataio.netcdf.metadata.Modis35ProfilePartReader;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A copy of the non-public NetCdfReadProfile in beam-netcdf module
 *
 * @author olafd
 */
public class Modis35NetCdfReadProfile {
    private Modis35ProfileInitPartReader profileInitPart;
    private final List<Modis35ProfilePartReader> profileParts = new ArrayList<Modis35ProfilePartReader>();

    public void setInitialisationPartReader(Modis35ProfileInitPartReader initPart) {
        this.profileInitPart = initPart;
    }

    public void addProfilePartReader(Modis35ProfilePartReader profilePart) {
        this.profileParts.add(profilePart);
    }

    public Product readProduct(final Modis35ProfileReadContext ctx) throws IOException {

        final Product product = profileInitPart.readProductBody(ctx);
        AbstractProductReader.configurePreferredTileSize(product);
        for (Modis35ProfilePartReader profilePart : profileParts) {
            profilePart.preDecode(ctx, product);
        }
        for (Modis35ProfilePartReader profilePart : profileParts) {
            profilePart.decode(ctx, product);
        }
        return product;
    }
}
