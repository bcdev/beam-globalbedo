def warp_and_write(outDir, ats_product, mer_product, warpParams, start_y, end_y):

    ats_product_name_list = ["reflec_nadir_0670","reflec_fward_0670"]
    mer_product_name_list = ["radiance_7"]

    #get dimensions
    height, width = mer_product.get_scene_height(), mer_product.get_scene_width()

    #Generate the output nc dataset
    rootgrp = Dataset(outDir, 'w', format='NETCDF4')
    rootgrp.createDimension('y', height)
    rootgrp.createDimension('x', width)
    latitudes = rootgrp.createVariable('latitude','f4',('y','x'), zlib=True, complevel=1)
    latitudes[:,:] = mer_product.get_band('latitude').read_as_array()
    longitudes = rootgrp.createVariable('longitude','f4',('y','x'), zlib=True, complevel=1)
    longitudes[:,:] = mer_product.get_band('longitude').read_as_array()

    #place the meris data into the file (only the bands currently)
    for variable in mer_product_name_list:
        temp = rootgrp.createVariable(variable,'f4',('y','x'), zlib=True, complevel=1)
        temp[:,:] = mer_product.get_band(variable).read_as_array()

    #warp aatsr data and place into the nc file
    for variable in ats_product_name_list:
        if "nadir" in variable:
            temp = rootgrp.createVariable(variable,'f4',('y','x'), zlib=True, complevel=1)
            temp[:,:] = warp(warpParams, ats_product.get_band(variable).read_as_array(512, end_y-start_y,
                                                                                      xoffset=0, yoffset=start_y),
                             [height,width], look='nadir')
        else:
            temp = rootgrp.createVariable(variable,'f4',('y','x'), zlib=True, complevel=1)
            temp[:,:] = warp(warpParams, ats_product.get_band(variable).read_as_array(512, end_y-start_y,
                                                                                      xoffset=0, yoffset=start_y),
                             [height,width], look='fward')