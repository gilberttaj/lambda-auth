# Comprehensive AWS Console Prerequisite Setup Guide for Private LambdaAuth

This guide provides detailed instructions for manually configuring the necessary AWS resources in the AWS Management Console. These resources are prerequisites for deploying the SAM (Serverless Application Model) template for the LambdaAuth project, which involves a private API Gateway and a Lambda function within a VPC.

**Target Audience:** Users who will be setting up the AWS infrastructure manually.
**Goal:** To have all networking and security components in place before SAM deployment.

---
Action: Create an Internet Gateway and attach it to your VPC.
Purpose: Provides internet connectivity to resources in your public subnets (specifically, the NAT Gateway).
Collect: Its ID (for reference).
NAT Gateway (NGW):
Action:
Allocate an Elastic IP address.
Create a NAT Gateway in one of your public subnets, associating the Elastic IP with it. For high availability, you can create a NAT Gateway in each public subnet (in different AZs) and configure route tables accordingly, but for simplicity, one can suffice for now.
Purpose: Allows your Lambda function (in private subnets) to initiate outbound connections to the internet (e.g., to reach AWS Cognito's public endpoints for JWKS, Google's endpoints if ever needed by server-side, or other public AWS service endpoints if not using VPC endpoints). It prevents inbound connections from the internet to your Lambda.
Collect: Its ID (for reference).
Route Tables:
Public Route Table:
Action: Create a route table. Associate it with your public subnets.
Configuration: Add a route:
Destination: 0.0.0.0/0
Target: Your Internet Gateway ID.
Private Route Table(s):
Action: Create one or more route tables. Associate them with your private subnets. (You can use one route table for multiple private subnets if they share the same routing needs).
Configuration: Add a route:
Destination: 0.0.0.0/0
Target: Your NAT Gateway ID.
II. Security

Security Groups (SGs):
Lambda Security Group:
Action: Create a Security Group for your Lambda function.
Purpose: Acts as a virtual firewall for your Lambda function's ENIs (Elastic Network Interfaces).
Configuration:
Inbound Rules:
Typically, no inbound rules are needed from the internet.
Allow traffic from the API Gateway VPC Endpoint's Security Group (see below) on the port your Lambda listens on (though for Lambda, it's more about network path than specific port listening from API Gateway). A common practice is to allow all traffic from the API Gateway VPC Endpoint's SG.
Alternatively, if the Lambda and API Gateway VPC endpoint share this SG or another common one, you might allow traffic from the SG itself.
Outbound Rules:
Allow HTTPS (TCP port 443) to destination 0.0.0.0/0. This is essential for the Lambda to:
Fetch the JWKS from Cognito's public URL to verify the ID token.
Make any other AWS SDK calls to services that don't have VPC endpoints configured (or if you choose not to use them).
Access any other external resources if needed.
Collect: The ID of this Security Group (e.g., sg-0123...). This will be used for the SecurityGroupIds parameter during SAM deployment.
API Gateway VPC Endpoint Security Group:
Action: Create a Security Group for the API Gateway VPC Endpoint.
Purpose: Controls traffic to and from the VPC endpoint for execute-api.
Configuration:
Inbound Rules:
Allow HTTPS (TCP port 443) from the source IP range of your clients (e.g., your corporate network CIDR, VPN client IP pool). This is critical for your Vue app (running on a client machine connected via VPN) to reach the private API.
Outbound Rules:
Can be left as default (allow all outbound), or if you want to be more restrictive, allow HTTPS (TCP port 443) to the subnets/IPs where your Lambda function's ENIs will reside (or to the Lambda Security Group).
Collect: Its ID (for reference and for configuring the Lambda SG if needed).
III. VPC Endpoints (for Private Connectivity to AWS Services)

API Gateway VPC Endpoint (Interface Endpoint for execute-api):
Action:
Go to VPC > Endpoints > Create Endpoint.
Service category: "AWS services".
Service name: Search for execute-api (e.g., com.amazonaws.<region>.execute-api).
Select your VPC.
Subnets: Select the private subnets where you want the endpoint to be accessible (typically the same ones your Lambda might use or where your clients can reach).
Enable Private DNS Name: Usually enabled.
Security group: Attach the API Gateway VPC Endpoint Security Group created in step 6.
Purpose: This is what makes your API Gateway truly private. It creates network interfaces in your specified private subnets, allowing resources within your VPC (or connected via VPN/Direct Connect) to access your API Gateway without traversing the public internet.
Collect: The VPC Endpoint ID (e.g., vpce-0123...). This will be used for the ApiGatewayVpcEndpointId parameter during SAM deployment.
(Highly Recommended) S3 Gateway Endpoint:
Action:
Go to VPC > Endpoints > Create Endpoint.
Service category: "AWS services".
Service name: Search for s3 (e.g., com.amazonaws.<region>.s3, type "Gateway").
Select your VPC.
Route tables: Select the route table(s) associated with your private subnets (where the Lambda runs).
Purpose: Allows your Lambda function to access S3 (e.g., for SAM to deploy the code package, or if your Lambda interacts with S3) without needing to go through the NAT Gateway. This is more secure and can save on NAT Gateway data processing costs.
(Optional but Recommended) CloudWatch Logs Interface Endpoint:
Action: Create an Interface VPC Endpoint for com.amazonaws.<region>.logs.
Associate it with your private subnets and an appropriate security group (e.g., the Lambda Security Group, or a dedicated one allowing HTTPS from Lambda SG).
Purpose: Allows your Lambda function to send logs to CloudWatch Logs without going through the NAT Gateway.
IV. Other AWS Services (You mentioned these are already configured, but for completeness):

AWS Cognito User Pool:
Ensure your User Pool is configured.
Ensure Google is set up as an Identity Provider.
Note your User Pool ID, App Client ID, and Cognito Domain (used in Vue app Amplify config).
IAM Roles & Policies:
The SAM template will attempt to create an IAM Role for the Lambda function. This role will automatically get AWSLambdaBasicExecutionRole (for CloudWatch Logs) and AWSLambdaVPCAccessExecutionRole (for VPC ENI management).
If your Lambda needs other permissions in the future (e.g., to access other AWS services), you'll modify this role.
Summary of items to collect for SAM deployment:

Private Subnet IDs (at least two)
Lambda Security Group ID
API Gateway VPC Endpoint ID
AllowedEmailDomains (you'll provide this string directly)
Before running sam deploy --guided: Make sure all the above networking components (VPC, Subnets, IGW, NGW, Route Tables, SGs, VPC Endpoints for API Gateway and S3) are created and correctly configured in the AWS Console. The SAM template relies on the existence and IDs of these pre-provisioned resources.

This manual setup is the most complex part. Once these are in place, the SAM deployment should be much smoother.

I will update the plan to reflect that we are detailing these manual AWS console configurations.