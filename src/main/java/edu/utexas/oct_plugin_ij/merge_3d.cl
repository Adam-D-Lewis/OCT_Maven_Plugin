#define COORD_SIZE 3
#define ONE 1
#define TWO 2
#define THREE 3
#define FOUR 4
#define FIVE 5
#define SIX 6

//Returns:
// .x = In bounds or not (0 or 1)
// .y = Linear coordinates
int2 inBounds(int* dims, 
				int* trans){
						
	if(	trans[0] < get_global_id(0) && get_global_id(0) < trans[0] + dims[0] && //make sure pixel is in the volume space
		trans[1] < get_global_id(1) && get_global_id(1) < trans[1] + dims[1] && //		^
		trans[2] < get_global_id(2) && get_global_id(2) < trans[2] + dims[2] ){ //        |
	   		int local_x = get_global_id(0) - trans[0];
	   		int local_y = get_global_id(1) - trans[1];
	   		int local_z = get_global_id(2) - trans[2];
	   		int a = local_x + local_y*dims[0] + local_z*dims[0]*dims[1];
	   		return (int2)(a, 1);
	   }else{
	   		return (int2)(0,0);
	   }
} 

__kernel void merge_1_to_1_u8(__global unsigned char *a,
								__global int* dims,
								__global int* trans,
								__global unsigned char *output){
	int l_dims[ONE*COORD_SIZE];
	int l_trans[ONE*COORD_SIZE];
	
	for(int i = 0; i < ONE*COORD_SIZE; i++){
		l_dims[i] = dims[i];
		l_trans[i] = trans[i];
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	int *aDims = &l_dims[0];			
				
	int *aTrans = &l_trans[0];
						
	int x = get_global_id(0);
	int y = get_global_id(1);
	int z = get_global_id(2);
							
	int lin = x + y*get_global_size(0) + z*get_global_size(0)*get_global_size(1);
	
	float output_f = 0;
	int avg = 0;
	
	int2 p = inBounds(aDims, aTrans);	
	if(p.y == 1){
		float l = a[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	output[lin] = (unsigned char)(output_f/avg);					
}

__kernel void merge_2_to_1_u8(__global unsigned char *a,
								__global unsigned char *b,
								__global int* dims,
								__global int* trans,
								__global unsigned char *output){
								
	int l_dims[6];
	int l_trans[6];
	
	for(int i = 0; i < TWO*COORD_SIZE; i++){
		l_dims[i] = dims[i];
		l_trans[i] = trans[i];
	}

	barrier(CLK_GLOBAL_MEM_FENCE);
			
	int *aDims = &l_dims[0];
	int *bDims = &l_dims[3];
	
	int *aTrans = &l_trans[0];
	int *bTrans = &l_trans[3];
	
	int x = get_global_id(0);
	int y = get_global_id(1);
	int z = get_global_id(2);
							
	int lin = x + y*get_global_size(0) + z*get_global_size(0)*get_global_size(1);
	
	float output_f = 0;
	int avg = 0;
	
	int2 p = inBounds(aDims, aTrans);	
	if(p.y == 1){
		float l = a[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(bDims, bTrans);	
	if(p.y == 1){
		float l = b[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}			
	
	output[lin] = (unsigned char)(output_f/avg);			
}

__kernel void merge_3_to_1_u8(__global unsigned char *a,
								__global unsigned char *b,
								__global unsigned char *c,
								__global int* dims,
								__global int* trans,
								__global unsigned char *output){
	
	int l_dims[THREE*COORD_SIZE];
	int l_trans[THREE*COORD_SIZE];

	for(int i = 0; i < THREE*COORD_SIZE; i++){
		l_dims[i] = dims[i];
		l_trans[i] = trans[i];
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);	
	
	int *aDims = &l_dims[0];
	int *bDims = &l_dims[3];
	int *cDims = &l_dims[6];
				
	int *aTrans = &l_trans[0];
	int *bTrans = &l_trans[3];
	int *cTrans = &l_trans[6];
						
	int x = get_global_id(0);
	int y = get_global_id(1);
	int z = get_global_id(2);
							
	int lin = x + y*get_global_size(0) + z*get_global_size(0)*get_global_size(1);
	
	float output_f = 0;
	int avg = 0;
	
	int2 p = inBounds(aDims, aTrans);	
	if(p.y == 1){
		float l = a[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(bDims, bTrans);	
	if(p.y == 1){
		float l = b[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(cDims, cTrans);	
	if(p.y == 1){
		float l = c[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	output[lin] = (unsigned char)(output_f/avg);
}

__kernel void merge_4_to_1_u8(__global unsigned char *a,
								__global unsigned char *b,
								__global unsigned char *c,
								__global unsigned char *d,
								__global int* dims,
								__global int* trans,
								__global unsigned char *output){
	
	int l_dims[FOUR*COORD_SIZE];
	int l_trans[FOUR*COORD_SIZE];

	for(int i = 0; i < FOUR*COORD_SIZE; i++){
		l_dims[i] = dims[i];
		l_trans[i] = trans[i];
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);	
	
	int *aDims = &l_dims[0];
	int *bDims = &l_dims[3];
	int *cDims = &l_dims[6];
	int *dDims = &l_dims[9];
				
	int *aTrans = &l_trans[0];
	int *bTrans = &l_trans[3];
	int *cTrans = &l_trans[6];
	int *dTrans = &l_trans[9];
						
	int x = get_global_id(0);
	int y = get_global_id(1);
	int z = get_global_id(2);
							
	int lin = x + y*get_global_size(0) + z*get_global_size(0)*get_global_size(1);
	
	float output_f = 0;
	int avg = 0;
	
	int2 p = inBounds(aDims, aTrans);	
	if(p.y == 1){
		float l = a[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(bDims, bTrans);	
	if(p.y == 1){
		float l = b[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(cDims, cTrans);	
	if(p.y == 1){
		float l = c[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(dDims, dTrans);	
	if(p.y == 1){
		float l = d[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	output[lin] = (unsigned char)(output_f/avg);
}

__kernel void merge_5_to_1_u8(__global unsigned char *a,
								__global unsigned char *b,
								__global unsigned char *c,
								__global unsigned char *d,
								__global unsigned char *e,
								__global int* dims,
								__global int* trans,
								__global unsigned char *output){
	
	int l_dims[FIVE*COORD_SIZE];
	int l_trans[FIVE*COORD_SIZE];

	for(int i = 0; i < FIVE*COORD_SIZE; i++){
		l_dims[i] = dims[i];
		l_trans[i] = trans[i];
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);	
	
	int *aDims = &l_dims[0];
	int *bDims = &l_dims[3];
	int *cDims = &l_dims[6];
	int *dDims = &l_dims[9];
	int *eDims = &l_dims[12];
				
	int *aTrans = &l_trans[0];
	int *bTrans = &l_trans[3];
	int *cTrans = &l_trans[6];
	int *dTrans = &l_trans[9];
	int *eTrans = &l_trans[12];
						
	int x = get_global_id(0);
	int y = get_global_id(1);
	int z = get_global_id(2);
							
	int lin = x + y*get_global_size(0) + z*get_global_size(0)*get_global_size(1);
	
	float output_f = 0;
	int avg = 0;
	
	int2 p = inBounds(aDims, aTrans);	
	if(p.y == 1){
		float l = a[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(bDims, bTrans);	
	if(p.y == 1){
		float l = b[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(cDims, cTrans);	
	if(p.y == 1){
		float l = c[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(dDims, dTrans);	
	if(p.y == 1){
		float l = d[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(eDims, eTrans);	
	if(p.y == 1){
		float l = e[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	output[lin] = (unsigned char)(output_f/avg);
}

__kernel void merge_6_to_1_u8(__global unsigned char *a,
								__global unsigned char *b,
								__global unsigned char *c,
								__global unsigned char *d,
								__global unsigned char *e,
								__global unsigned char *f,
								__global int* dims,
								__global int* trans,
								__global unsigned char *output){
	
	int l_dims[SIX*COORD_SIZE];
	int l_trans[SIX*COORD_SIZE];

	for(int i = 0; i < SIX*COORD_SIZE; i++){
		l_dims[i] = dims[i];
		l_trans[i] = trans[i];
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);	
	
	int *aDims = &l_dims[0];
	int *bDims = &l_dims[3];
	int *cDims = &l_dims[6];
	int *dDims = &l_dims[9];
	int *eDims = &l_dims[12];
	int *fDims = &l_dims[15];				
				
	int *aTrans = &l_trans[0];
	int *bTrans = &l_trans[3];
	int *cTrans = &l_trans[6];
	int *dTrans = &l_trans[9];
	int *eTrans = &l_trans[12];
	int *fTrans = &l_trans[15];
						
	int x = get_global_id(0);
	int y = get_global_id(1);
	int z = get_global_id(2);
							
	int lin = x + y*get_global_size(0) + z*get_global_size(0)*get_global_size(1);
	
	float output_f = 0;
	int avg = 0;
	
	int2 p = inBounds(aDims, aTrans);	
	if(p.y == 1){
		float l = a[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(bDims, bTrans);	
	if(p.y == 1){
		float l = b[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(cDims, cTrans);	
	if(p.y == 1){
		float l = c[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(dDims, dTrans);	
	if(p.y == 1){
		float l = d[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(eDims, eTrans);	
	if(p.y == 1){
		float l = e[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	p = inBounds(fDims, fTrans);	
	if(p.y == 1){
		float l = f[p.x];
		output_f += l;
		avg += (l == 0) ? 0 : 1;
	}
	
	output[lin] = (unsigned char)(output_f/avg);
}
