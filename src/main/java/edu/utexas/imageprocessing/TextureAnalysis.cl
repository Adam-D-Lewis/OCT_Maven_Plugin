//Note: GLCM_SIZE will be modified
//via text editor
#define GLCM_SIZE 64*64
#define GLCM_AXIS 64

__kernel void TextureAnalysis_u8(__global unsigned char *Grayscale,
				 int RadiusX,
				 int RadiusY, 
				 int NumberOfOffsets,
				 __global int *XOffset,
				 __global int *YOffset,
				 __global float *Contrast,
				 __global float *Energy,
				 __global float *Correlation,
				 __global float *Homogeneity,
				 __global float *Entropy,
				 __global float *MaxProb,
				 __global float *ClusterShade,
				 __global float *ClusterProm){
	
	int x = get_global_id(2);
	int y = get_global_id(1);
	int z = get_global_id(0);
	
	int xo = get_global_offset(2);
	int yo = get_global_offset(1);
	int zo = get_global_offset(0);
	
	int linear = y*get_global_size(2) + x;
	
	if(x < RadiusX || (int)get_global_size(2) - RadiusX < x ||
		y < RadiusY || (int)get_global_size(1) - RadiusY < y){
		
		Contrast[linear] = 0;
		Energy[linear] = 0;
		Homogeneity[linear] = 0;
		Entropy[linear] = 0;
		Correlation[linear] = 0;
		MaxProb[linear] = 0;
		ClusterShade[linear] = 0;
		ClusterProm[linear] = 0;
		return;
	}
	
	unsigned char local_Grayscale[GLCM_SIZE];
	int local_XOffset[32];
	int local_YOffset[32];
	
	int SizeX = 2*RadiusX + 1;
    int SizeY = 2*RadiusY + 1;	
	
	for(int i = 0; i < NumberOfOffsets; i++){
		local_XOffset[i] = XOffset[i];
		local_YOffset[i] = YOffset[i];
	}
	
	for(int j = -RadiusY; j <= RadiusY; j++){
		for(int i = -RadiusX; i <= RadiusX; i++){
			int linear_in = (y + j)*get_global_size(2) + (x + i); 
			local_Grayscale[(j + RadiusY)*SizeX + (i + RadiusX)] = Grayscale[linear_in]/4;
		}
	}	
	
	int Counter = 0;
	float Total = 0;

	unsigned char GLCMIndexI[GLCM_SIZE];
	unsigned char GLCMIndexJ[GLCM_SIZE];
	float GLCMValue[GLCM_SIZE];
	
	for(int i = 0; i < GLCM_SIZE; i++){
		GLCMIndexI[i] = 0;
		GLCMIndexJ[i] = 0;
		GLCMValue[i] = 0;
	}

	float GLCMMeanI = 0;
	float GLCMMeanJ = 0;
	float GLCMStdDevI = 0;
	float GLCMStdDevJ = 0;

    for (int j = -RadiusY; j <= RadiusY; j++){
        for (int i = -RadiusX; i <= RadiusX; i++){
            //adjusted for the different in Grayscale vs. Contrast/Energy/etc. size
            int index = (i + RadiusX) + SizeX*(j + RadiusY);
            unsigned char N = local_Grayscale[index];
            
            for (int l = 0; l < NumberOfOffsets; l++){
                if(0 <= i + RadiusX + local_XOffset[l] && i + RadiusX + local_XOffset[l] < SizeX &&
                   0 <= j + RadiusY + local_YOffset[l] && j + RadiusY + local_YOffset[l] < SizeY){
                    
                    //this handles looping through the pixel offsets
                    int index1 = (i + local_XOffset[l] + RadiusX) + SizeX*(j + local_YOffset[l] + RadiusY);
                    unsigned char N1 = local_Grayscale[index1];
                    
                    bool PairExists = false;
                    
                    //Check if the I,J pair already exists
                    for (int q = 0; q < Counter; q++){
                        if ((GLCMIndexI[q] == N && GLCMIndexJ[q] == N1)){
                            GLCMValue[q] += 1;
                            PairExists = true;
                        }
                    }
                    
                    //The Pair Doesn't Exist
                    if (!PairExists){
                        GLCMIndexI[Counter] = N;
                        GLCMIndexJ[Counter] = N1;
                        GLCMValue[Counter] += 1;
                        Counter += 1;
                    }
                    
                    Total += 1;
                }else{
                    //Do nothing, out of bounds
                }
            }
        }
    }
		
	float lContrast = 0;
	float lEnergy = 0;
	float lHomogeneity = 0;
	float lEntropy = 0;
	float lCorrelation = 0;
	float lMaxProb = 0;
	float lClusterShade = 0;
	float lClusterProm = 0;
	float p = 0;

	//Compute the Mean for I and J
	for (int l = 0; l < Counter; l++){
		GLCMValue[l] /= Total;
	
		float I = GLCMIndexI[l];
		GLCMMeanI += GLCMValue[l] * I;

		float J = GLCMIndexJ[l];
		GLCMMeanJ += GLCMValue[l] * J;
	}

	for (int l = 0; l < Counter; l++){
		GLCMStdDevI += pow((float)GLCMValue[l] - GLCMMeanI, 2);
		GLCMStdDevJ += pow((float)GLCMValue[l] - GLCMMeanJ, 2);
	}

	GLCMStdDevI = sqrt(GLCMStdDevI);
	GLCMStdDevJ = sqrt(GLCMStdDevJ);

	for (int l = 0; l < Counter; l++){
		int N_I = GLCMIndexI[l];
		int N_J = GLCMIndexJ[l];

		p = GLCMValue[l];
		float iMinusj = fabs((float)N_I - (float)N_J);

		lContrast += p*iMinusj*iMinusj;
		lEnergy += p*p;
		lHomogeneity += p / (1 + iMinusj*iMinusj);

		if (GLCMStdDevI*GLCMStdDevJ != 0){
			lCorrelation += (N_I*N_J*p - (GLCMMeanI*GLCMMeanJ)) / (GLCMStdDevI*GLCMStdDevJ);
		}

		lEntropy += p*log(p);
		lClusterShade += pow(N_I + N_J - GLCMMeanJ - GLCMMeanI, 3)*p;
		lClusterProm += pow(N_I + N_J - GLCMMeanJ - GLCMMeanI, 4)*p;

		lMaxProb = max(p, lMaxProb);
	}

	Contrast[linear] = lContrast;
	Energy[linear] = lEnergy;
	Homogeneity[linear] = lHomogeneity;
	Entropy[linear] = -lEntropy;
	Correlation[linear] = lCorrelation;
	MaxProb[linear] = lMaxProb;
	ClusterShade[linear] = lClusterShade;
	ClusterProm[linear] = lClusterProm;
}
