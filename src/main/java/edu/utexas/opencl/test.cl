__kernel void add_32f(__global float* A,
						__global float* B,
						__global float* C){
					
	size_t x = get_global_id(0);
	size_t y = get_global_id(1);
						
	long lin = x + y*get_global_size(0);
	
	C[lin] = A[lin] + B[lin];						
} 