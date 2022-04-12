import os
import boto3

BUCKET_NAME = os.getenv("S3_BUCKET_NAME")

if __name__ == '__main__':
	print("upload s3")
	s3 = boto3.resource("s3")
	bucket = s3.Bucket(BUCKET_NAME)
	bucket.upload_file(
		Filename="proserve-cloudformation-stack.zip",
		Key="proserve-cloudformation-stack.zip"
	)

