//#define M_PI_F 3.14159f

float to_radians(float degrees) {
    return degrees * (M_PI_F/180.0f);
}

__kernel void rotate_u8(__global unsigned char* input, 
												float xAng,
												float yAng,
												float zAng,
												int xInSize,
												int yInSize,
												int zInSize,
							__global unsigned char* output){
	
	int halfXOut = get_global_size(0) / 2;
	int halfYOut = get_global_size(1) / 2;
	int halfZOut = get_global_size(2) / 2;
	
	//Add in offsets to make things about 0,0,0
	int i = (get_global_id(0) - halfXOut);  //x
	int j = (get_global_id(1) - halfYOut);  //y
	int k = (get_global_id(2) - halfZOut);  //z

    float inArrayBounds = xInSize*yInSize*zInSize;
    
    int xDiff = (get_global_size(0) - xInSize)/2;
    int yDiff = (get_global_size(1) - yInSize)/2;
    int zDiff = (get_global_size(2) - zInSize)/2;

    // Converts angles from degrees to radians.
    float alpha = to_radians(xAng);
    float beta = to_radians(yAng);
    float gamma = to_radians(zAng);
    
    float Voxel[6] = {0,0,0};
    
	// The following code performs the mathematical matrix rotation.
	float xr1 = i*(cos(beta)*cos(gamma));
    float xr2 = j*(cos(gamma)*sin(alpha)*sin(beta)-cos(alpha)*sin(gamma));
    float xr3 = k*(cos(alpha)*cos(gamma)*sin(beta)+sin(alpha)*sin(gamma));
    Voxel[0] = floor(xr1 + xr2 + xr3);
    Voxel[1] = ceil(xr1 + xr2 + xr3);
    float yr1 = i*(cos(beta)*sin(gamma));
    float yr2 = j*(cos(alpha)*cos(gamma) + sin(alpha)*sin(beta)*sin(gamma));
    float yr3 = k*(-cos(gamma)*sin(alpha) + cos(alpha)*sin(beta)*sin(gamma));
    Voxel[2] = floor(yr1 + yr2 + yr3);
    Voxel[3] = ceil(yr1 + yr2 + yr3);
    float zr1 = i*(-sin(beta));
    float zr2 = j*(cos(beta)*sin(alpha));
    float zr3 = k*(cos(alpha)*cos(beta));
    Voxel[4] = floor(zr1 + zr2 + zr3);
    Voxel[5] = ceil(zr1 + zr2 + zr3);

    int outLinCoord =  get_global_id(0) +
    					(get_global_id(1))*get_global_size(0) +  
    					(get_global_id(2))*get_global_size(0)*get_global_size(1);

    //Voxel[0,1,2] -> Voxel[x,y,z] and is the from where in the input array to fetch the data to be placed in coord[0,1,2] -> coord[x,y,z]
    //Voxel[...] may be negative needs to be adjusted with offsets, and checked.
    //Voxel[...] may be larger than input array coords. If out of bounds, make the output value 0 for these voxels

	int iIn1 = (Voxel[0] + halfXOut - xDiff);
	int iIn2 = (Voxel[1] + halfXOut - xDiff);
	int jIn1 = (Voxel[2] + halfYOut - yDiff);
	int jIn2 = (Voxel[3] + halfYOut - yDiff);	
	int kIn1 = (Voxel[4] + halfZOut - zDiff);
	int kIn2 = (Voxel[5] + halfZOut - zDiff);

	if(0 < iIn1 && iIn1 < xInSize &&
		0 < iIn2 && iIn2 < xInSize &&
		0 < jIn1 && jIn1 < yInSize &&
		0 < jIn2 && jIn2 < yInSize &&
		0 < kIn1 && kIn1 < zInSize &&
		0 < kIn2 && kIn2 < zInSize){
		
		int coord[8];
		
		coord[0] = iIn1 + jIn1*xInSize + kIn1*xInSize*yInSize;
		coord[1] = iIn1 + jIn2*xInSize + kIn1*xInSize*yInSize;
		coord[2] = iIn1 + jIn1*xInSize + kIn2*xInSize*yInSize;
		coord[3] = iIn1 + jIn2*xInSize + kIn2*xInSize*yInSize;
		coord[4] = iIn2 + jIn1*xInSize + kIn1*xInSize*yInSize;
		coord[5] = iIn2 + jIn1*xInSize + kIn2*xInSize*yInSize;
		coord[6] = iIn2 + jIn2*xInSize + kIn1*xInSize*yInSize;
		coord[7] = iIn2 + jIn2*xInSize + kIn2*xInSize*yInSize;
		
		float interp = 0;
		for(int p = 0; p < 8; p++){
			interp += input[coord[p]];
		}
		
		interp /= 8;
		
		output[outLinCoord] = (unsigned char)interp;
    }else{
    	output[outLinCoord] = 0;
    }
}