# Data source per AMI Ubuntu
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]  # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Key Pair (SSH)
resource "aws_key_pair" "k3s" {
  key_name   = "${var.project_name}-k3s-key"
  public_key = file("${path.module}/k3s-key.pub")
}

# IAM Role per EC2
resource "aws_iam_role" "k3s" {
  name = "${var.project_name}-k3s-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

# IAM Policy per ECR pull
resource "aws_iam_role_policy_attachment" "ecr" {
  role       = aws_iam_role.k3s.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# IAM Policy per CloudWatch logs
resource "aws_iam_role_policy_attachment" "cloudwatch" {
  role       = aws_iam_role.k3s.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# Instance Profile
resource "aws_iam_instance_profile" "k3s" {
  name = "${var.project_name}-k3s-profile"
  role = aws_iam_role.k3s.name
}

# EC2 Instance
resource "aws_instance" "k3s" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  key_name               = aws_key_pair.k3s.key_name
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.k3s.id]
  iam_instance_profile   = aws_iam_instance_profile.k3s.name

  root_block_device {
    volume_size           = 30
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  user_data = file("${path.module}/user_data.sh")

  tags = {
    Name = "${var.project_name}-k3s-node"
  }
}

# Elastic IP
resource "aws_eip" "k3s" {
  instance = aws_instance.k3s.id
  domain   = "vpc"

  tags = {
    Name = "${var.project_name}-k3s-eip"
  }
}