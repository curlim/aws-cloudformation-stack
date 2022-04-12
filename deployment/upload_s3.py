import os
import boto3

BUCKET_NAME = os.getenv("S3_BUCKET_NAME")

if __name__ == '__main__':
	ref = os.getenv("GITHUB_REF").split('/')
	if ref[1] != 'tags':
		print("non tagged ref")
		return
	else
		version = ref[2]
	print(f"upload version {version} s3")
	s3_client = boto3.client("s3")
	s3 = boto3.resource("s3")
	bucket = s3.Bucket(BUCKET_NAME)
	bucket.upload_file(
		Filename="proserve-cloudformation-stack.zip",
		Key="proserve-cloudformation-stack.zip"
	)

