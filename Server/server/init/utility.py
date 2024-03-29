import sys, os 
import ast
import hmac
import datetime


sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from models import UserModel

# JWT 토큰 생성 함수
def authenticate(user_email, password):
    
    user = UserModel.query.filter_by(email=user_email).first()
    user_dict = ast.literal_eval(str(user))
    if user_dict['password'] and hmac.compare_digest(user_dict['password'].encode('utf-8'), password.encode('utf-8')):
        return user_dict

# JWT 토큰 인증 함수
def identity(payload):
    user_id = payload['identity']
    
    return UserModel.query.get(user_id)

