import os, sys
import datetime
import json
import hashlib

from flask import request, Blueprint, Response, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity, create_access_token, create_refresh_token
from flask_jwt_extended import get_jwt

# custom 
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from models import UserModel, TokenBlocklist, UserRefreshToken
from init.init_db import rdb
# 웬만한 jwt 객체 설정에 대한 것들은 jwt.utility에 있다. 
from init.init_jwt import jwt, SECRET_KEY

bp = Blueprint('users', __name__, url_prefix='/users')

blacklist = set()


@jwt.token_in_blocklist_loader
def check_if_token_in_blocklist(jwt_header, jwt_payload):
    jti = jwt_payload['jti']
    token = TokenBlocklist.query.filter_by(token=jti).first()
    return bool(token)

def is_username_taken(username):
    existing_user = UserModel.query.filter_by(username=username).first()
    return existing_user is not None


@bp.route('/register', methods=['POST'])
def register():
    user_info = request.get_json()
    if not user_info:
        return jsonify({"status_code": 400, "message": "Bad Request"}), 400

    pw_receive = str(user_info['password'])
    nickname_receive = str(user_info['nickname'])
    email_receive = user_info['email']

    # Check if username is taken
    if is_username_taken(nickname_receive):
        return jsonify({"status_code": 409, "message": "Username is already taken"}), 409

    pw_hash = hashlib.sha256(pw_receive.encode()).hexdigest()
    user = UserModel(username=nickname_receive, email=email_receive, password=pw_hash)
    rdb.session.add(user)
    rdb.session.commit()

    return jsonify({"status_code": 200, "message": "Success"}), 200

@bp.route('/email_check', methods=['POST'])
def email_check():
    user_info = request.get_json()
    if not user_info:
        return jsonify({"status_code": 400, "message": "Bad Request"}), 400
    
    email_receive = user_info['email']
    existing_user = UserModel.query.filter_by(email=email_receive).first()
    if existing_user:
        return jsonify({"status_code": 409, "message": "Email is already taken"}), 409

    return jsonify({"status_code": 200, "message": "Email is available"}), 200

@bp.route('/login', methods=["POST"])
def login():
    user_info = request.get_json()
    email_receive = user_info['email']
    pw_receive = user_info['password']

    pw_hash = hashlib.sha256(pw_receive.encode()).hexdigest()
    result = UserModel.query.filter(UserModel.email == email_receive, UserModel.password == pw_hash).first()

    if result:
        UserRefreshToken.query.filter_by(user_id=result.user_id).delete()
        rdb.session.commit()

        # Create Access, Refresh token
        access_token = create_access_token(identity=result.user_id, fresh=False)
        refresh_token = create_refresh_token(identity=result.user_id)

        # Token DB에 저장 
        token = UserRefreshToken(token=refresh_token, user_id=result.user_id)
        rdb.session.add(token)
        rdb.session.commit()

        return jsonify({'msg': 'Login in successfully', 'access_token': access_token, 'username': result.username}), 200
    else:
        return jsonify({'result': 'fail', 'message': '아이디/비밀번호가 일치하지 않습니다.'}), 401


@bp.route('/logout', methods=['GET'])
@jwt_required()
def logout():
    # Get JWT ID of the access token 
    jti = get_jwt()['jti']
    token = TokenBlocklist(jti=jti)
    rdb.session.add(token)
    rdb.session.commit()
    return jsonify({'msg': 'Successfully logged out'}), 200


@bp.route('/refresh', methods=["GET"])
@jwt_required()
def refresh():
    current_user_id = get_jwt_identity()

    # Get the user's refresh token from the database
    token = UserRefreshToken.query.filter_by(user_id=current_user_id).first()

    if not token or not token.is_valid():
        return jsonify({"msg": "Invalid refresh token"}), 401

    # Create a new access token
    access_token = create_access_token(identity=current_user_id, fresh=False)

    return jsonify({"access_token": access_token}), 200


@bp.route('/protected', methods=["GET"])
@jwt_required()
def protected():
    user_id = get_jwt_identity()
    # user = UserModel.query.filter_by(email=user_email).first()
    return json.dumps({'msg': f'hello {user_id}'})